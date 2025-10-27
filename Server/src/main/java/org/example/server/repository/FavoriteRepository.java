package org.example.server.repository;

import org.example.server.entity.Favorite;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    long countByProduct(Product product);
    @Query(value = "SELECT f FROM Favorite f " +
            "JOIN FETCH f.product p " +
            "LEFT JOIN FETCH p.category " +
            "WHERE f.user = :user",
            countQuery = "SELECT COUNT(f) FROM Favorite f WHERE f.user = :user") // Cần countQuery riêng
    Page<Favorite> findByUserWithProductAndCategory(@Param("user") User user, Pageable pageable);
    Page<Favorite> findByUser(User user, Pageable pageable);
}
