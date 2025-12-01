package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.CategoryRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.PromotionRepository;
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
    private final CategoryRepository categoryRepo;

    public List<Promotion> getAll() {
        return promoRepo.findAll();
    }

    public Promotion getOne(Long id) {
        return promoRepo.findById(id).orElseThrow(() -> new RuntimeException("Promotion not found"));
    }

    @Transactional
    public Promotion savePromotion(Map<String, Object> body, Long id) {
        Promotion p = (id != null) ? getOne(id) : new Promotion();

        if (body.containsKey("code")) {
            String code = (String) body.get("code");
            if (id == null && code != null && promoRepo.findByCodeIgnoreCase(code).isPresent()) {
                throw new RuntimeException("Code already exists");
            }
            if (id == null) p.setCode(code);
        }

        if (body.containsKey("name")) p.setName((String) body.get("name"));
        if (body.containsKey("active")) p.setActive(Boolean.valueOf(body.get("active").toString()));

        if (body.containsKey("type")) p.setType(Promotion.PromoType.valueOf((String) body.get("type")));
        if (body.containsKey("value")) p.setValue(new BigDecimal(body.get("value").toString()));
        if (body.containsKey("minOrderTotal")) p.setMinOrderTotal(new BigDecimal(body.get("minOrderTotal").toString()));

        if (body.containsKey("maxUses")) {
            String max = String.valueOf(body.get("maxUses"));
            p.setMaxUses((max == null || max.isEmpty() || max.equals("null")) ? null : Integer.parseInt(max));
        }

        if (body.containsKey("startAt")) {
            String s = (String) body.get("startAt");
            p.setStartAt((s == null || s.isEmpty()) ? null : LocalDateTime.parse(s));
        }
        if (body.containsKey("endAt")) {
            String s = (String) body.get("endAt");
            p.setEndAt((s == null || s.isEmpty()) ? null : LocalDateTime.parse(s));
        }

        if (body.containsKey("scope")) {
            String scopeStr = (String) body.get("scope");
            Promotion.PromoScope scope = Promotion.PromoScope.valueOf(scopeStr);
            p.setScope(scope);

            p.setCategory(null);
            p.setProducts(null);

            if (scope == Promotion.PromoScope.CATEGORY && body.containsKey("categoryId")) {
                Long catId = Long.valueOf(body.get("categoryId").toString());
                Category cat = categoryRepo.findById(catId).orElseThrow(() -> new RuntimeException("Category not found"));
                p.setCategory(cat);
            }
            else if (scope == Promotion.PromoScope.PRODUCT && body.containsKey("productIds")) {
                List<?> rawIds = (List<?>) body.get("productIds");
                Set<Long> pIds = rawIds.stream().map(x -> Long.valueOf(x.toString())).collect(Collectors.toSet());
                List<Product> products = productRepo.findAllById(pIds);
                p.setProducts(new HashSet<>(products));
            }
        }

        return promoRepo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        promoRepo.deleteById(id);
    }

    public static record ApplyResult(BigDecimal discount, Promotion promotion, String message) {}

    @Transactional(readOnly = true)
    public ApplyResult preview(String code, List<OrderItem> items) {
        if (code == null || code.trim().isEmpty()) return new ApplyResult(BigDecimal.ZERO, null, "Code required");

        Promotion p = promoRepo.findByCodeWithDetails(code.trim()).orElse(null);
        if (p == null) return new ApplyResult(BigDecimal.ZERO, null, "Invalid code");

        String error = validate(p);
        if (error != null) return new ApplyResult(BigDecimal.ZERO, null, error);

        Set<Long> pIds = items.stream().map(i -> i.getProduct().getId()).collect(Collectors.toSet());
        Map<Long, Product> productMap = productRepo.findAllById(pIds).stream().collect(Collectors.toMap(Product::getId, pr -> pr));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal eligibleTotal = BigDecimal.ZERO;

        for (OrderItem item : items) {
            Product realProduct = productMap.get(item.getProduct().getId());
            if (realProduct == null) continue;

            BigDecimal lineTotal = realProduct.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            if (isEligible(p, realProduct)) {
                eligibleTotal = eligibleTotal.add(lineTotal);
            }
        }

        if (p.getMinOrderTotal() != null && subtotal.compareTo(p.getMinOrderTotal()) < 0) {
            return new ApplyResult(BigDecimal.ZERO, null, "Min order total not met");
        }

        if (eligibleTotal.compareTo(BigDecimal.ZERO) == 0) {
            return new ApplyResult(BigDecimal.ZERO, null, "Code not applicable to these items");
        }

        BigDecimal discount = calculateDiscount(p, eligibleTotal);
        if (discount.compareTo(subtotal) > 0) discount = subtotal;

        return new ApplyResult(discount, p, "Applied successfully");
    }

    private String validate(Promotion p) {
        if (Boolean.FALSE.equals(p.getActive())) return "Code is inactive";

        LocalDateTime now = LocalDateTime.now();
        if (p.getStartAt() != null && now.isBefore(p.getStartAt())) return "Code not yet active";
        if (p.getEndAt() != null && now.isAfter(p.getEndAt())) return "Code expired";

        if (p.getMaxUses() != null && p.getUsedCount() != null && p.getUsedCount() >= p.getMaxUses()) {
            return "Code usage limit reached";
        }
        return null;
    }

    private boolean isEligible(Promotion p, Product product) {
        if (p.getScope() == Promotion.PromoScope.ALL) return true;

        if (p.getScope() == Promotion.PromoScope.CATEGORY) {
            return p.getCategory() != null && product.getCategory() != null
                    && p.getCategory().getId().equals(product.getCategory().getId());
        }

        if (p.getScope() == Promotion.PromoScope.PRODUCT) {
            if (p.getProducts() == null || p.getProducts().isEmpty()) return false;
            return p.getProducts().stream().anyMatch(allowed -> allowed.getId().equals(product.getId()));
        }
        return false;
    }

    private BigDecimal calculateDiscount(Promotion p, BigDecimal baseAmount) {
        if (p.getType() == Promotion.PromoType.FIXED) {
            return p.getValue();
        } else {
            return baseAmount.multiply(p.getValue()).divide(BigDecimal.valueOf(100));
        }
    }

    @Transactional
    public void increaseUsage(Promotion p) {
        if (p == null || p.getId() == null) return;
        promoRepo.findById(p.getId()).ifPresent(current -> {
            current.setUsedCount((current.getUsedCount() == null ? 0 : current.getUsedCount()) + 1);
            promoRepo.save(current);
        });
    }
}