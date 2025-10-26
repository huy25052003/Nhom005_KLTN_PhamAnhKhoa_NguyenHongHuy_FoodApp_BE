package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.OrderRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Order placeOrder(String username, List<OrderItem> items) {
        return placeOrder(username, items, null);
    }

    @Transactional
    public Order placeOrder(String username, List<OrderItem> items, String promoCode) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // set giá từ DB + kiểm tồn
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem i : items) {
            Product p = productRepo.findById(i.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + i.getProduct().getId()));
            if (p.getStock() < i.getQuantity()) {
                throw new RuntimeException("Out of stock for product: " + p.getId());
            }
            i.setPrice(p.getPrice());
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        Promotion applied = null;
        if (promoCode != null && !promoCode.isBlank()) {
            var res = promotionService.preview(promoCode, items);
            discount = res.discount();
            applied  = res.promotion(); // có thể null nếu mã ko hợp lệ
        }

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        Order order = Order.builder()
                .user(user)
                .total(total)
                .discount(discount)
                .promotionCode(applied != null ? applied.getCode() : null)
                .status("PENDING")
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setItems(items);

        Order saved = orderRepo.save(order);

        // Trừ tồn
        for (OrderItem i : saved.getItems()) {
            Product p = productRepo.findById(i.getProduct().getId()).orElseThrow();
            p.setStock(p.getStock() - i.getQuantity());
            productRepo.save(p);
        }

        // tăng dùng mã
        if (applied != null) promotionService.increaseUsage(applied);
        notificationService.newOrderNotify(saved);
        return saved;
    }


    @Transactional( )
    public List<Order> getUserOrders(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepo.findByUser(user);
    }

    @Transactional( )
    public Order getOne(Authentication auth, Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !order.getUser().getUsername().equals(auth.getName())) {
            throw new RuntimeException("Forbidden");
        }
        // chạm lazy
        if (order.getShipping() != null) order.getShipping().getId();
        order.getItems().forEach(oi -> {
            if (oi.getProduct() != null) oi.getProduct().getId();
        });
        return order;
    }

    @Transactional( )
    public Page<Order> getAllOrders(int page, int size) {
        return orderRepo.findAll(PageRequest.of(page, size));
    }


    @Transactional
    public Order updateStatus(Long id, String nextRaw) {
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        String cur  = normalize(o.getStatus());
        String next = normalize(nextRaw);

        if (Objects.equals(cur, next)) return o; // idempotent

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
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Forbidden");
        }
        String cur = normalize(order.getStatus());
        if (!"PENDING".equals(cur)) {
            throw new RuntimeException("Only PENDING order can be cancelled");
        }
        order.setStatus("CANCELED");
        Order saved = orderRepo.save(order);

        for (OrderItem i : saved.getItems()) {
            Product p = productRepo.findById(i.getProduct().getId())
                    .orElseThrow();
            p.setStock(p.getStock() + i.getQuantity());
            productRepo.save(p);
        }
        return saved;
    }
}
