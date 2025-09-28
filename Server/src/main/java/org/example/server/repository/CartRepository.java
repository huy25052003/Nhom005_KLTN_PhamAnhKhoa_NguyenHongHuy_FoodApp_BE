package org.example.server.repository;

import org.example.server.entity.Cart;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
    select c from Cart c
    left join fetch c.items i
    left join fetch i.product p
    left join fetch p.category cat
    where c.user.username = :username
  """)
    Optional<Cart> findByUsernameWithItemsAndProduct(String username);

    Optional<Cart> findByUser(User user);
}
