package org.example.server.repository;

import org.example.server.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Chỉ lấy sản phẩm đang hiện (cho trang chủ)
    List<Product> findByActiveTrue();
    // Tìm các món có calo thấp hơn mức cho phép và đang Active
    List<Product> findByActiveTrueAndCaloriesLessThanEqual(Integer maxCalories);
    // (Optional) Tìm các món trong khoảng calo (ví dụ +/- 100 calo so với mục tiêu)
    List<Product> findByActiveTrueAndCaloriesBetween(Integer min, Integer max);
    // Search cho KHÁCH (chỉ tìm active = true)
    @Query("""
    SELECT p FROM Product p
    WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
      AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
      AND p.active = true
    """)
    Page<Product> searchPublic(@Param("categoryId") Long categoryId,
                               @Param("q") String q,
                               Pageable pageable);

    // Search cho ADMIN (tìm tất cả, kể cả ẩn)
    @Query("""
    SELECT p FROM Product p
    WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
      AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Product> searchAdmin(@Param("categoryId") Long categoryId,
                              @Param("q") String q,
                              Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "JOIN p.orderItems oi " +
            "JOIN oi.order o " +
            "WHERE p.active = true AND o.status = 'DONE' " +
            "GROUP BY p " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Product> findTopSelling(Pageable pageable);
}
