// org/example/server/repository/StatsRepository.java
package org.example.server.repository;

import org.example.server.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<Product, Long> {

    @Query("""
      select coalesce(sum(oi.price * oi.quantity), 0),
             count(distinct o.id),
             coalesce(sum(oi.quantity), 0)
      from Order o
      join o.items oi
      where o.status in ('CONFIRMED','PREPARING','SHIPPING','COMPLETED')
        and o.createdAt >= :start and o.createdAt < :end
    """)
    List<Object[]> overview(@Param("start") LocalDateTime start,
                            @Param("end")   LocalDateTime end);

    @Query("""
      select function('date', o.createdAt) as d,
             coalesce(sum(oi.price * oi.quantity), 0)
      from Order o
      join o.items oi
      where o.status in ('CONFIRMED','PREPARING','SHIPPING','COMPLETED')
        and o.createdAt >= :start and o.createdAt < :end
      group by function('date', o.createdAt)
      order by d asc
    """)
    List<Object[]> revenueByDay(@Param("start") LocalDateTime start,
                                @Param("end")   LocalDateTime end);

    @Query("""
      select oi.product.id, oi.product.name,
             coalesce(sum(oi.quantity), 0) as qty,
             coalesce(sum(oi.price * oi.quantity), 0) as revenue
      from Order o
      join o.items oi
      where o.status in ('CONFIRMED','PREPARING','SHIPPING','COMPLETED')
        and o.createdAt >= :start and o.createdAt < :end
      group by oi.product.id, oi.product.name
      order by qty desc
    """)
    List<Object[]> topProducts(@Param("start") LocalDateTime start,
                               @Param("end")   LocalDateTime end);

    @Query("""
      select o.status, count(o)
      from Order o
      where o.createdAt >= :start and o.createdAt < :end
      group by o.status
    """)
    List<Object[]> ordersByStatus(@Param("start") LocalDateTime start,
                                  @Param("end")   LocalDateTime end);

    @Query("""
      select p from Product p
      where p.stock <= :threshold
      order by p.stock asc
    """)
    List<Product> lowStock(@Param("threshold") int threshold);
}
