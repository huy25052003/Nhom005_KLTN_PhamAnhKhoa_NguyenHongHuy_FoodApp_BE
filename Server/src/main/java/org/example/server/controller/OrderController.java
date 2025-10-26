package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    public record PlaceOrderRequest(
            List<OrderItem> items,
            Map<String, Object> shippingInfo,
            String paymentMethod,
            String promoCode
            ){}

    @PostMapping
    public ResponseEntity<Order> placeOrder(Authentication auth, @RequestBody PlaceOrderRequest request) throws Exception {
        return ResponseEntity.ok(orderService.placeOrder(auth.getName(),
                request.items(),
                request.promoCode(),
                request.shippingInfo(),
                request.paymentMethod()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getUserOrders(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOne(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOne(auth, id));
    }

    @GetMapping
    public ResponseEntity<Page<Order>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.updateStatus(id, status);
    }
    // NEW: user huỷ đơn khi còn PENDING
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Order> cancel(Authentication auth, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(auth.getName(), id));
    }
}
