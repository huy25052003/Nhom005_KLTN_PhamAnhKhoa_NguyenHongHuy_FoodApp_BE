package org.example.server.repository;

import org.example.server.entity.Order;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    @Query(value = "SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.shipping s ",
            countQuery = "SELECT COUNT(o) FROM Order o") // Cần countQuery riêng khi dùng FETCH với Pageable
    Page<Order> findAllWithUserDetails(Pageable pageable);

    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH p.category c " + // Fetch category của product
            "LEFT JOIN FETCH o.shipping s " +
            "LEFT JOIN FETCH o.user u " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.user u " +
            "LEFT JOIN FETCH o.items i " +
            "LEFT JOIN FETCH i.product p " +
            "WHERE o.status IN :statuses " +
            "ORDER BY o.createdAt ASC")
    List<Order> findByStatusInWithDetails(@Param("statuses") List<String> statuses);

    @Query("""
        SELECT COUNT(o) > 0 
        FROM Order o 
        JOIN o.items oi 
        WHERE o.user = :user 
          AND oi.product = :product 
          AND o.status IN ('DONE')
    """)
    boolean existsFinishedOrderWithProduct(@Param("user") User user, @Param("product") Product product);

    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);

    @Query("""
        SELECT new map(
            oi.product.id as productId, 
            oi.product.name as productName, 
            SUM(oi.quantity) as totalQuantity
        )
        FROM OrderItem oi
        JOIN oi.order o
        WHERE o.status IN ('CONFIRMED', 'PREPARING')
        GROUP BY oi.product.id, oi.product.name
    """)
    List<Map<String, Object>> getKitchenAggregatedItems();
}
