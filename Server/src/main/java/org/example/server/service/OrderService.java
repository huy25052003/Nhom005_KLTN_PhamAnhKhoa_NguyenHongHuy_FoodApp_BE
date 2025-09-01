package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

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

    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        ensureTransitionAllowed(order.getStatus(), status);
        order.setStatus(status);
        return orderRepo.save(order);
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
