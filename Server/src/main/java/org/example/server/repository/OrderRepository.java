package org.example.server.repository;

import org.example.server.entity.Order;
import org.example.server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser(User user);
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH o.shipping s " +
            "WHERE o.user = :user " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByUserWithItems(@Param("user") User user);
    @Query(value = "SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.shipping s "+
            "LEFT JOIN FETCH o.items i "+
            "LEFT JOIN FETCH i.product p ",
            countQuery = "SELECT COUNT(o) FROM Order o") // Cần countQuery riêng khi dùng FETCH với Pageable
    Page<Order> findAllWithUserDetails(Pageable pageable);

    // Thêm phương thức này cho getOne để fetch tất cả
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH p.category c " + // Fetch category của product
            "LEFT JOIN FETCH o.shipping s " +
            "LEFT JOIN FETCH o.user u " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);
}
