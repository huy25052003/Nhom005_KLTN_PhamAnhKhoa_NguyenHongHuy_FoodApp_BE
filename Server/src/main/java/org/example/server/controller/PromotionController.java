package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.OrderItem;
import org.example.server.entity.Product;
import org.example.server.repository.ProductRepository;
import org.example.server.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final ProductRepository productRepo;

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("code", ""));
        List<Map<String,Object>> items = (List<Map<String,Object>>) body.getOrDefault("items", List.of());

        List<OrderItem> list = new ArrayList<>();
        for (Map<String,Object> m : items) {
            Long pid = Long.valueOf(String.valueOf(m.get("productId")));
            Integer q  = Integer.valueOf(String.valueOf(m.getOrDefault("quantity", 1)));
            Product p  = new Product(); p.setId(pid);
            OrderItem oi = OrderItem.builder().product(p).quantity(q).build();
            list.add(oi);
        }
        var res = promotionService.preview(code, list);
        return ResponseEntity.ok(Map.of(
                "discount", res.discount(),
                "message",  res.message()
        ));
    }
}
