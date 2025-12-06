package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Order;
import org.example.server.repository.OrderRepository;
import org.example.server.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class KitchenController {
    private final OrderService  orderService;
    private final OrderRepository orderRepository;

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getKitchenOrders() {
        return ResponseEntity.ok(orderService.getKitchenOrders());
    }

    @GetMapping("/aggregated")
    public ResponseEntity<List<Map<String, Object>>> getAggregatedItems() {
        return ResponseEntity.ok(orderRepository.getKitchenAggregatedItems());
    }
    @PutMapping("/items/{itemId}/status")
    public ResponseEntity<?> updateItemStatus(@PathVariable Long itemId, @RequestParam String status, Authentication auth) {
        return ResponseEntity.ok(orderService.updateItemStatus(itemId, status, auth));
    }
    @PostMapping("/orders/{id}/claim")
    public ResponseEntity<Void> claimOrder(@PathVariable Long id, Authentication auth) {
        orderService.claimOrder(id, auth.getName());
        return ResponseEntity.ok().build();
    }
    @PostMapping("/orders/{id}/finish")
    public ResponseEntity<Void> finishOrder(@PathVariable Long id, Authentication auth) {
        orderService.finishOrder(id, auth.getName());
        return ResponseEntity.ok().build();
    }
}
