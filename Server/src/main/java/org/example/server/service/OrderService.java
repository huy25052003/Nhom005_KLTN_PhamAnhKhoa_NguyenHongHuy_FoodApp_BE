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
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Danh sách món trống");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem i : items) {
            Product p = productRepo.findById(i.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + i.getProduct().getId()));

            if (p.getStock() < i.getQuantity()) {
                throw new RuntimeException("Out of stock for product: " + p.getId());
            }
            i.setPrice(p.getPrice());

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
        ShippingInfo shippingSnap = shippingInfoService.snapshotForOrder(user);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .status("PENDING")
                .shipping(shippingSnap)
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setItems(items);

        Order saved = orderRepo.save(order);

        for (OrderItem i : saved.getItems()) {
            Product p = productRepo.findById(i.getProduct().getId())
                    .orElseThrow();
            p.setStock(p.getStock() - i.getQuantity());
            productRepo.save(p);
        }
        return saved;
    }


    @Transactional(readOnly = true)
    public List<Order> getUserOrders(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepo.findByUser(user);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
