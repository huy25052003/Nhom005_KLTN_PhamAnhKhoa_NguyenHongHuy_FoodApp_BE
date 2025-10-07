package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.PromotionRepository;
import org.example.server.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promoRepo;
    private final ProductRepository productRepo;

    public static record ApplyResult(BigDecimal discount, Promotion promotion, String message) {}

    /** Tính discount cho 1 danh sách OrderItem thô (productId + quantity + price sẽ set lại theo DB) */
    public ApplyResult preview(String code, List<OrderItem> items) {
        if (code == null || code.trim().isEmpty()) {
            return new ApplyResult(BigDecimal.ZERO, null, "Không có mã khuyến mãi.");
        }
        Promotion p = promoRepo.findByCodeIgnoreCase(code.trim())
                .orElse(null);
        if (p == null) return new ApplyResult(BigDecimal.ZERO, null, "Mã không tồn tại.");

        String invalid = validate(p);
        if (invalid != null) return new ApplyResult(BigDecimal.ZERO, null, invalid);

        // Lấy lại products & giá hiện tại
        Map<Long, Product> pmap = productRepo.findAllById(
                items.stream().map(it -> it.getProduct().getId()).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Product::getId, x->x));

        // Tổng tạm tính + subtotal eligible
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal eligible = BigDecimal.ZERO;

        for (OrderItem i : items) {
            Product prod = pmap.get(i.getProduct().getId());
            if (prod == null) continue;
            BigDecimal line = prod.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            subtotal = subtotal.add(line);
            if (isEligible(p, prod)) eligible = eligible.add(line);
        }

        if (p.getMinOrderTotal() != null && subtotal.compareTo(p.getMinOrderTotal()) < 0) {
            return new ApplyResult(BigDecimal.ZERO, null, "Chưa đạt tổng tối thiểu.");
        }

        BigDecimal discount = calcDiscount(p, eligible);
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ApplyResult(BigDecimal.ZERO, null, "Mã hợp lệ nhưng không tạo ra giảm giá.");
        }
        return new ApplyResult(discount, p, "Áp mã thành công.");
    }

    private String validate(Promotion p) {
        if (Boolean.FALSE.equals(p.getActive())) return "Mã đang tắt.";
        LocalDateTime now = LocalDateTime.now();
        if (p.getStartAt()!=null && now.isBefore(p.getStartAt())) return "Mã chưa bắt đầu.";
        if (p.getEndAt()!=null && now.isAfter(p.getEndAt())) return "Mã đã hết hạn.";
        if (p.getMaxUses()!=null && p.getUsedCount()!=null && p.getUsedCount()>=p.getMaxUses()) return "Mã đã hết lượt dùng.";
        if (p.getType()== Promotion.PromoType.PERCENT) {
            if (p.getValue()==null || p.getValue().compareTo(BigDecimal.ZERO)<0 || p.getValue().compareTo(new BigDecimal("100"))>0)
                return "Phần trăm không hợp lệ.";
        } else if (p.getType()== Promotion.PromoType.FIXED) {
            if (p.getValue()==null || p.getValue().compareTo(BigDecimal.ZERO)<=0)
                return "Giá trị giảm không hợp lệ.";
        }
        return null;
    }

    private boolean isEligible(Promotion p, Product prod) {
        return switch (p.getScope()) {
            case ALL -> true;
            case CATEGORY -> p.getCategory()!=null && prod.getCategory()!=null
                    && Objects.equals(p.getCategory().getId(), prod.getCategory().getId());
            case PRODUCT -> p.getProducts()!=null && p.getProducts().stream().anyMatch(x -> Objects.equals(x.getId(), prod.getId()));
        };
    }

    private BigDecimal calcDiscount(Promotion p, BigDecimal eligibleSubtotal) {
        if (eligibleSubtotal.compareTo(BigDecimal.ZERO)<=0) return BigDecimal.ZERO;
        return switch (p.getType()) {
            case PERCENT -> eligibleSubtotal.multiply(p.getValue()).divide(new BigDecimal("100"));
            case FIXED -> p.getValue().min(eligibleSubtotal);
        };
    }

    @Transactional
    public void increaseUsage(Promotion p) {
        if (p==null) return;
        if (p.getUsedCount()==null) p.setUsedCount(0);
        p.setUsedCount(p.getUsedCount()+1);
        promoRepo.save(p);
    }
}
