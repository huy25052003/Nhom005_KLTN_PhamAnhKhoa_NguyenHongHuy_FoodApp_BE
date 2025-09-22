package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService stats;

    @GetMapping("/overview")
    public ResponseEntity<Map<String,Object>> overview(
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to) {
        return ResponseEntity.ok(stats.overview(from, to));
    }

    @GetMapping("/revenue-series")
    public ResponseEntity<List<Map<String,Object>>> revenueSeries(
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to) {
        return ResponseEntity.ok(stats.revenueSeries(from, to));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String,Object>>> topProducts(
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to,
            @RequestParam(defaultValue="10") int limit) {
        return ResponseEntity.ok(stats.topProducts(from, to, limit));
    }

    @GetMapping("/orders-by-status")
    public ResponseEntity<List<Map<String,Object>>> ordersByStatus(
            @RequestParam(required=false) String from,
            @RequestParam(required=false) String to) {
        return ResponseEntity.ok(stats.ordersByStatus(from, to));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String,Object>>> lowStock(
            @RequestParam(defaultValue="10") int threshold,
            @RequestParam(defaultValue="20") int limit) {
        return ResponseEntity.ok(stats.lowStock(threshold, limit));
    }
}
