package org.example.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "promotions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã code (tuỳ chọn). Nếu có → user nhập mã. Nếu null → khuyến mãi tự động.
    @Column(unique = true)
    private String code;

    private String name;

    // PERCENT (0..100) hoặc FIXED (đ)
    @Enumerated(EnumType.STRING)
    private PromoType type;

    @Column(precision = 18, scale = 2)
    private BigDecimal value; // % hoặc số tiền

    // Áp điều kiện tổng tối thiểu (tuỳ chọn)
    @Column(precision = 18, scale = 2)
    private BigDecimal minOrderTotal;

    // Phạm vi áp dụng: toàn shop / theo category / theo danh sách product
    @Enumerated(EnumType.STRING)
    private PromoScope scope; // ALL, CATEGORY, PRODUCT

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category; // nếu scope=CATEGORY

    @ManyToMany
    @JoinTable(name = "promotion_products",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> products; // nếu scope=PRODUCT

    private Boolean active = true;

    private LocalDateTime startAt;  // có thể null
    private LocalDateTime endAt;    // có thể null

    private Integer maxUses;   // giới hạn dùng tổng, null = không giới hạn
    private Integer usedCount; // auto tăng khi dùng

    public enum PromoType { PERCENT, FIXED }
    public enum PromoScope { ALL, CATEGORY, PRODUCT }
}
