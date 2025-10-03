package org.example.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String paymentMethod = "PAYOS"; // Mặc định là PayOS

    @Column(length = 100)
    private String payosOrderId; // ID đơn hàng từ PayOS

    @Column(length = 100)
    private String payosPaymentLinkId; // ID link thanh toán từ PayOS

    @Column(length = 200)
    private String status; // PENDING, PAID, FAILED, CANCELLED

    @Column(length = 500)
    private String payosSignature; // Chữ ký từ PayOS để verify

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}