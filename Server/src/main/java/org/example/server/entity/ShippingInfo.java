package org.example.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipping_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shipping_user", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_shipping_user", columnList = "user_id")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ShippingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String addressLine;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
