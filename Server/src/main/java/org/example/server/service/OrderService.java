package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.*;
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

        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .user(user)
                .total(total)
                .status("PENDING")
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setItems(items);

        return orderRepo.save(order);
    }

    public List<Order> getUserOrders(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepo.findByUser(user);
    }

    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    public Order updateStatus(Long orderId, String status) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        return orderRepo.save(order);
    }
}
