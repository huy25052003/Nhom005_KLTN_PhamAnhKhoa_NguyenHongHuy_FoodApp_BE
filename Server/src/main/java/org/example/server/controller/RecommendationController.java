package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<Product>> getRecommendations(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(recommendationService.getRecommendedProducts(auth));
    }
}