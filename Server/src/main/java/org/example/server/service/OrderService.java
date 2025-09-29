package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.*;
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
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELED"),
            "CONFIRMED", Set.of("PREPARING", "CANCELED"),
            "PREPARING", Set.of("DELIVERING", "DONE"),
            "DELIVERING", Set.of("DONE"),
            "DONE", Set.of(),
            "CANCELED", Set.of()
    );
    public Order placeOrder(String username, List<OrderItem> items) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

        Order order = Order.builder()
                .user(user)
                .total(total)
                .status("PENDING")
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

    public List<Order> getUserOrders(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepo.findByUser(user);
    }

    public Order getOne(Authentication auth, Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !order.getUser().getUsername().equals(auth.getName())) {
            throw new RuntimeException("Forbidden");
        }
        return order;
    }

    public Page<Order> getAllOrders(int page, int size) {
        return orderRepo.findAll(PageRequest.of(page, size));
    }

    private void ensureTransitionAllowed(String from, String to) {
        switch (from) {
            case "PENDING" -> {
                if (!(to.equals("PAID") || to.equals("CANCELLED"))) {
                    throw new RuntimeException("Invalid transition PENDING -> " + to);
                }
            }
            case "PAID" -> {
                if (!(to.equals("SHIPPED"))) {
                    throw new RuntimeException("Invalid transition PAID -> " + to);
                }
            }
            case "SHIPPED" -> {
                if (!(to.equals("DELIVERED"))) {
                    throw new RuntimeException("Invalid transition SHIPPED -> " + to);
                }
            }
            default -> throw new RuntimeException("Order is final, cannot change from " + from);
        }
    }

    @Transactional
    public Order updateStatus(Long id, String nextRaw) {
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        String cur  = normalize(o.getStatus());
        String next = normalize(nextRaw);

        if (cur.equals(next)) return o; // idempotent

        Set<String> nexts = ALLOWED.getOrDefault(cur, Set.of());
        if (!nexts.contains(next)) {
            throw new RuntimeException("Invalid transition " + cur + " -> " + next);
        }

        o.setStatus(next);
        o.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(o);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    public Order cancel(String username, Long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Forbidden");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Only PENDING order can be cancelled");
        }
        order.setStatus("CANCELLED");
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
