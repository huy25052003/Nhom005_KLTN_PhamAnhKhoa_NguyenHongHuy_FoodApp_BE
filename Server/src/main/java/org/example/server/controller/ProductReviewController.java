package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Review;
import org.example.server.repository.ReviewRepository;
import org.example.server.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ReviewRepository.ReviewView>> list(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.listByProduct(productId));
    }

    @PostMapping
    public ResponseEntity<Review> create(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        int rating = body.get("rating") == null ? 0 : Integer.parseInt(String.valueOf(body.get("rating")));
        String comment = body.get("comment") == null ? "" : String.valueOf(body.get("comment"));
        return ResponseEntity.ok(reviewService.create(productId, rating, comment, auth));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long productId, @PathVariable Long reviewId) {
        reviewService.delete(auth, reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avg")
    public ResponseEntity<Map<String, Object>> avg(@PathVariable Long productId) {
        double avg = reviewService.getAverageRating(productId);
        return ResponseEntity.ok(Map.of("productId", productId, "avgRating", avg));
    }
}