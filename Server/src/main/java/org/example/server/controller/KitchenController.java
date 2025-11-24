package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Order;
import org.example.server.repository.OrderRepository;
import org.example.server.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
