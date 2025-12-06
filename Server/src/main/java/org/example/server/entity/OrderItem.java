package org.example.server.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantity;

    private BigDecimal price;


    @Builder.Default
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chef_id")
    @JsonIgnoreProperties({"password", "roles", "orders", "cart", "tokens"})
    private User chef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference("order-item")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"items","category"})
    private Product product;

    @PrePersist
    void prePersist() {
        if (status == null) status = "PENDING";
    }
}
