package org.example.server.repository;

import org.example.server.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCase(String code);
    @Query("SELECT p FROM Promotion p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.products " +
            "WHERE lower(p.code) = lower(:code)")
    Optional<Promotion> findByCodeWithDetails(@Param("code") String code);
}