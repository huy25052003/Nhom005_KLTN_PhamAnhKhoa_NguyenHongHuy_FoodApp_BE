package org.example.server.repository;

import org.example.server.entity.Favorite;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    long countByProduct(Product product);

    Page<Favorite> findByUser(User user, Pageable pageable);
}
