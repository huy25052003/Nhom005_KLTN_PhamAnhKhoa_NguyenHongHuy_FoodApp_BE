package org.example.server.repository;

import org.example.server.entity.Review;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    public interface ReviewView {
        Long getId();
        Integer getRating();
        String getComment();
        java.time.Instant getCreatedAt();
        String getUserName();
    }

    @Query("""
        select r.id as id,
               r.rating as rating,
               r.comment as comment,
               r.createdAt as createdAt,
               u.username as userName
        from Review r
        join r.user u
        where r.product.id = :pid
        order by r.createdAt desc
    """)
    List<ReviewView> findViewByProductId(@Param("pid") Long productId);

    boolean existsByUserAndProductId(User user, Long productId);

    @Query("select coalesce(avg(r.rating),0) from Review r where r.product.id = :pid")
    Double avgRating(@Param("pid") Long productId);
}
