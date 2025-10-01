package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.entity.Review;
import org.example.server.entity.User;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.ReviewRepository;
import org.example.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<ReviewRepository.ReviewView> listByProduct(Long productId) {
        return reviewRepo.findViewByProductId(productId);
    }

    @Transactional
    public Review create(Authentication auth, Long productId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating phải 1..5");
        }

        User user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

         if (reviewRepo.existsByUserAndProductId(user, productId)) {
             throw new RuntimeException("Bạn đã đánh giá sản phẩm này.");
         }

        Review r = new Review();
        r.setUser(user);
        r.setProduct(product);
        r.setRating(rating);
        r.setComment(comment == null ? "" : comment.trim());
        r.setCreatedAt(Instant.now());
        return reviewRepo.save(r);
    }

    @Transactional
    public void delete(Authentication auth, Long reviewId) {
        Review r = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        boolean isOwner = r.getUser() != null && r.getUser().getUsername().equals(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Không có quyền xoá review này");
        }
        reviewRepo.delete(r);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Long productId) {
        return reviewRepo.avgRating(productId);
    }
}
