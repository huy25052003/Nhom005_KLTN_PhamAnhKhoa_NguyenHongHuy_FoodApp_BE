package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.OrderRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable; // Thêm import
import org.springframework.data.domain.Sort; // Thêm import
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "PENDING",    Set.of("CONFIRMED", "CANCELED", "CANCELLED"),
            "CONFIRMED",  Set.of("PREPARING", "CANCELED", "CANCELLED"),
            "PREPARING",  Set.of("DELIVERING", "DONE", "CANCELED", "CANCELLED"),
            "DELIVERING", Set.of("DONE", "CANCELED", "CANCELLED"),
            "DONE",       Set.of(),
            "CANCELED",   Set.of(),
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

        BigDecimal discount = BigDecimal.ZERO;
        Promotion applied = null;
        if (promoCode != null && !promoCode.isBlank()) {
            var res = promotionService.preview(promoCode, items);
            discount = res.discount();
            applied = res.promotion();
        }

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getName().equals(username)) {
            throw new RuntimeException("Authentication context error");
        }

        shippingInfoService.upsertMy(auth, shippingData);
        ShippingInfo shippingSnapshot = shippingInfoService.snapshotForOrder(user);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .discount(discount)
                .promotionCode(applied != null ? applied.getCode() : null)
                .status("PENDING")
                .paymentMethod(paymentMethod.toUpperCase())
                .shipping(shippingSnapshot)
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setItems(items);

        Order saved = orderRepo.save(order);

        for (OrderItem i : saved.getItems()) {
            Product p = i.getProduct();
            p.setStock(p.getStock() - i.getQuantity());
            productRepo.save(p);
        }
        if (applied != null) promotionService.increaseUsage(applied);
        notificationService.newOrderNotify(saved);

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
        if (!isAdmin && !order.getUser().getUsername().equals(auth.getName())) {
            throw new RuntimeException("Forbidden");
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
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        String cur  = normalize(o.getStatus());
        String next = normalize(nextRaw);

        if (Objects.equals(cur, next)) return o;

        Set<String> nexts = ALLOWED.getOrDefault(cur, Set.of());
        if (!nexts.contains(next)) {
            throw new RuntimeException("Invalid transition " + cur + " -> " + next);
        }

        o.setStatus(next);
        o.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(o);
    }

    private String normalize(String s) {
        if (s == null) return "";
        String up = s.trim().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(up)) return "CANCELED";
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
        order.setStatus("CANCELED");
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepo.save(order);
        
        for (OrderItem i : saved.getItems()) {
            Product p = i.getProduct();
            if (p == null) {
                System.err.println("Warning: Product not found for OrderItem ID: " + i.getId() + " during cancellation of Order ID: " + orderId);
                continue;
            }
            p.setStock(p.getStock() + i.getQuantity());
            productRepo.save(p);
        }
        return saved;
    }
}