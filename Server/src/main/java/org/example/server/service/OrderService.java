package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.OrderItemRepository;
import org.example.server.repository.OrderRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final CartService cartService;
    private final ShippingInfoService shippingInfoService;
    private final PromotionService promotionService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderItemRepository orderItemRepository;

    // Quy định chuyển trạng thái hợp lệ
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "PENDING",    Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED",  Set.of("PREPARING", "CANCELLED"),
            "PREPARING",  Set.of("DELIVERING", "DONE", "CANCELLED"),
            "DELIVERING", Set.of("DONE", "CANCELLED"),
            "DONE",       Set.of(),
            "CANCELLED",  Set.of()
    );

    @Transactional
    public Order placeOrder(String username, List<OrderItem> items, String promoCode, Map<String, Object> shippingData, String paymentMethod) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem i : items) {
            Product p = productRepo.findById(i.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + i.getProduct().getId()));
            if (p.getStock() < i.getQuantity()) {
                throw new RuntimeException("Out of stock for product: " + p.getName());
            }
            i.setPrice(p.getPrice());
            i.setProduct(p);
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }

        // --- 1. TÍNH GIẢM GIÁ TỪ COUPON ---
        BigDecimal promoDiscount = BigDecimal.ZERO;
        Promotion applied = null;
        if (promoCode != null && !promoCode.isBlank()) {
            var res = promotionService.preview(promoCode, items);
            promoDiscount = res.discount();
            applied = res.promotion();
        }

        // --- 2. TÍNH GIẢM GIÁ TỪ HẠNG THÀNH VIÊN ---
        BigDecimal memberDiscount = BigDecimal.ZERO;
        int points = user.getPoints() == null ? 0 : user.getPoints();
        double rate = 0;

        if (points >= 2000) {
            rate = 0.08; // Kim Cương: 8%
        } else if (points >= 500) {
            rate = 0.05; // Vàng: 5%
        } else if (points >= 100) {
            rate = 0.03; // Bạc: 3%
        } else {
            rate = 0.01; // Đồng: 1%
        }

        memberDiscount = subtotal.multiply(BigDecimal.valueOf(rate));

        // Tổng giảm giá
        BigDecimal totalDiscount = promoDiscount.add(memberDiscount);

        BigDecimal total = subtotal.subtract(totalDiscount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getName().equals(username)) {
            throw new RuntimeException("Authentication context error");
        }

        shippingInfoService.upsertMy(auth, shippingData);
        ShippingInfo shippingSnapshot = shippingInfoService.snapshotForOrder(user);

        // Lưu mã khuyến mãi đặc biệt nếu dùng hạng thành viên
        String finalPromoCode = applied != null ? applied.getCode() : (rate > 0 ? "MEMBER_RANK" : null);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .discount(totalDiscount)
                .promotionCode(finalPromoCode)
                .status("PENDING")
                .paymentMethod(paymentMethod.toUpperCase())
                .shipping(shippingSnapshot)
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setItems(items);

        Order saved = orderRepo.save(order);

        // Trừ tồn kho
        for (OrderItem i : saved.getItems()) {
            Product p = i.getProduct();
            p.setStock(p.getStock() - i.getQuantity());
            productRepo.save(p);
        }
        // Tăng lượt dùng mã giảm giá
        if (applied != null) promotionService.increaseUsage(applied);

        notificationService.newOrderNotify(saved);
        if (user.getEmail() != null && Boolean.TRUE.equals(user.getIsEmailVerified())) {
            emailService.sendOrderConfirmation(user.getEmail(), saved);
        }
        cartService.clear(auth);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepo.findByUserWithItems(user);
    }

    @Transactional(readOnly = true)
    public Order getOne(Authentication auth, Long id) {
        Order order = orderRepo.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isKitchen = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_KITCHEN"));
        boolean isOwner = order.getUser().getUsername().equals(auth.getName());

        if (!isAdmin && !isKitchen && !isOwner) {
            throw new AccessDeniedException("Forbidden");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepo.findAllWithUserDetails(pageable);
    }

    @Transactional
    public Order updateStatus(Long id, String nextRaw) {
        Order o = orderRepo.findByIdWithDetails(id).orElseThrow(NoSuchElementException::new);
        String cur  = normalize(o.getStatus());
        String next = normalize(nextRaw);

        if (Objects.equals(cur, next)) return o;

        Set<String> nexts = ALLOWED.getOrDefault(cur, Set.of());
        if (!nexts.contains(next)) {
            throw new RuntimeException("Invalid transition " + cur + " -> " + next);
        }

        o.setStatus(next);
        o.setUpdatedAt(LocalDateTime.now());

        // --- QUAN TRỌNG: CỘNG ĐIỂM KHI ĐƠN HÀNG HOÀN TẤT (DONE) ---
        if ("DONE".equals(next)) {
            User user = o.getUser();
            // Quy đổi: 10.000đ = 1 điểm (Làm tròn xuống)
            if (o.getTotal() != null && o.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                int pointsEarned = o.getTotal().divide(BigDecimal.valueOf(10000), 0, RoundingMode.FLOOR).intValue();

                if (pointsEarned > 0) {
                    int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
                    user.setPoints(currentPoints + pointsEarned);
                    userRepo.save(user);
                }
            }
        }
        // ----------------------------------------------------------

        if ("PENDING".equals(cur) && "CONFIRMED".equals(next)) {
            notificationService.notifyKitchenOfNewOrder(o);
        }
        return orderRepo.save(o);
    }

    private String normalize(String s) {
        if (s == null) return "";
        String up = s.trim().toUpperCase(Locale.ROOT);
        if ("CANCELED".equals(up)) return "CANCELLED";
        return up;
    }

    @Transactional
    public Order cancel(String username, Long orderId) {
        Order order = orderRepo.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Forbidden");
        }
        String cur = normalize(order.getStatus());
        if (!"PENDING".equals(cur)) {
            throw new RuntimeException("Only PENDING order can be cancelled");
        }

        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepo.save(order);

        // Hoàn lại kho
        for (OrderItem i : saved.getItems()) {
            Product p = i.getProduct();
            if (p != null) {
                p.setStock(p.getStock() + i.getQuantity());
                productRepo.save(p);
            }
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Order> getKitchenOrders() {
        List<String> statuses = List.of("CONFIRMED", "PREPARING");
        return orderRepo.findByStatusInWithDetails(statuses);
    }

    @Transactional
    public OrderItem updateItemStatus(Long itemId, String status, Authentication auth) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Món không tồn tại"));

        User currentChef = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COOKING".equals(status)) {
            if (item.getChef() != null && !item.getChef().getId().equals(currentChef.getId())) {
                throw new RuntimeException("Món này đã có người nhận rồi!");
            }
            item.setChef(currentChef);
        }

        if ("PENDING".equals(status)) {
            item.setChef(null);
        }

        item.setStatus(status);
        OrderItem savedItem = orderItemRepository.save(item);

        // Cập nhật trạng thái đơn cha dựa trên các món con
        Order order = item.getOrder();
        List<OrderItem> allItems = order.getItems();

        boolean allDone = allItems.stream().allMatch(i -> "DONE".equals(i.getStatus()));
        boolean anyCooking = allItems.stream().anyMatch(i -> "COOKING".equals(i.getStatus()) || "DONE".equals(i.getStatus()));

        if (allDone) {
            order.setStatus("DELIVERING");
        } else if (anyCooking) {
            order.setStatus("PREPARING");
        } else {
            order.setStatus("CONFIRMED");
        }
        orderRepo.save(order);

        // Bắn socket báo frontend
        messagingTemplate.convertAndSend("/topic/kitchen/update", "UPDATE");

        return savedItem;
    }

    @Transactional
    public void claimOrder(Long orderId, String username) {
        // 1. Lấy thông tin đơn hàng và đầu bếp
        Order order = orderRepo.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User chef = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Chef not found"));

        boolean updated = false;
        // 2. Duyệt qua các món chưa nhận (PENDING) và chuyển sang COOKING
        for (OrderItem item : order.getItems()) {
            if ("PENDING".equals(item.getStatus())) {
                item.setStatus("COOKING");
                item.setChef(chef);
                orderItemRepository.save(item);
                updated = true;
            }
        }

        // 3. Cập nhật trạng thái đơn hàng và bắn thông báo
        if (updated) {
            order.setStatus("PREPARING");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepo.save(order);
            messagingTemplate.convertAndSend("/topic/kitchen/update", "CLAIM_ORDER");
        }
    }
}