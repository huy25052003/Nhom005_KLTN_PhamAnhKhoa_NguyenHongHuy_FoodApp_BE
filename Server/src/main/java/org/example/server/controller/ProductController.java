package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductController {
    private final ProductService productService;

    // Admin lấy tất cả
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // API Mới: Khách lấy list (chỉ Active)
    @GetMapping("/public")
    public ResponseEntity<List<Product>> getPublicProducts() {
        return ResponseEntity.ok(productService.getPublicProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return ResponseEntity.ok(productService.createProduct(product));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id); // Gọi hàm xóa mềm
        return ResponseEntity.noContent().build();
    }

    // API Mới: Toggle Ẩn/Hiện nhanh
    @PatchMapping("/{id}/toggle")
    @Transactional
    public ResponseEntity<Void> toggleProduct(@PathVariable Long id) {
        productService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean isAdmin // Thêm cờ isAdmin
    ) {
        Page<Product> pg = productService.search(categoryId, q, page, limit, isAdmin);
        return ResponseEntity.ok(Map.of(
                "items", pg.getContent(),
                "total", pg.getTotalElements()
        ));
    }
    @GetMapping("/top")
    public ResponseEntity<List<Product>> getTopProducts(@RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(productService.getTopSellingProducts(limit));
    }
}