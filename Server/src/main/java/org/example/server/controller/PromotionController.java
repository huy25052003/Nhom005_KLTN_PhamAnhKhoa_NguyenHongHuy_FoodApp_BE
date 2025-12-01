package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.OrderItem;
import org.example.server.entity.Product;
import org.example.server.entity.Promotion;
import org.example.server.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("code", ""));
        List<Map<String,Object>> itemsMap = (List<Map<String,Object>>) body.getOrDefault("items", List.of());

        List<OrderItem> list = new ArrayList<>();
        for (Map<String,Object> m : itemsMap) {
            Object pidObj = m.get("productId");
            // Bỏ qua nếu không có productId
            if (pidObj == null) continue;

            Long pid = Long.valueOf(String.valueOf(pidObj));
            Integer q  = Integer.valueOf(String.valueOf(m.getOrDefault("quantity", 1)));

            Product p  = new Product(); p.setId(pid);
            OrderItem oi = OrderItem.builder().product(p).quantity(q).build();
            list.add(oi);
        }

        var res = promotionService.preview(code, list);

        // --- SỬA LỖI TẠI ĐÂY ---
        // Sử dụng HashMap thay vì Map.of để tránh lỗi NullPointerException khi code = null
        Map<String, Object> response = new HashMap<>();
        response.put("discount", res.discount());
        response.put("message", res.message());
        response.put("code", res.promotion() != null ? res.promotion().getCode() : null);

        return ResponseEntity.ok(response);
    }

    // ... (Giữ nguyên các API admin: getAll, create, update, delete) ...
    @GetMapping
    public ResponseEntity<List<Promotion>> getAll() {
        return ResponseEntity.ok(promotionService.getAll());
    }

    @PostMapping
    public ResponseEntity<Promotion> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(promotionService.savePromotion(body, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Promotion> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(promotionService.savePromotion(body, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}