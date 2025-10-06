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
    public Review create(Long productId, int rating, String comment, Authentication auth) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating phải từ 1 đến 5");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        User user;
        if (auth != null && auth.isAuthenticated()) {
            user = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            if (reviewRepo.existsByUserAndProductId(user, productId)) {
                throw new RuntimeException("Bạn đã đánh giá sản phẩm này.");
            }
        } else {
            user = userRepo.findByUsername("guest")
                    .orElseThrow(() -> new RuntimeException("Tài khoản khách không tồn tại"));
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Vui lòng đăng nhập để xóa đánh giá");
        }

        boolean isOwner = r.getUser() != null && r.getUser().getUsername().equals(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isGuestReview = r.getUser() != null && r.getUser().getUsername().equals("guest");
        if (!isOwner && !isAdmin && !isGuestReview) {
            throw new RuntimeException("Không có quyền xóa đánh giá này");
        }
        reviewRepo.delete(r);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Long productId) {
        return reviewRepo.avgRating(productId);
    }
}