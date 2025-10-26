package org.example.server.repository;

import org.example.server.entity.Order;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser(User user);
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH o.shipping s " +
            "WHERE o.user = :user " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserWithItems(@Param("user") User user);
}
