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
}
