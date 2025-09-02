package org.example.server.repository;

import org.example.server.entity.Cart;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
}
