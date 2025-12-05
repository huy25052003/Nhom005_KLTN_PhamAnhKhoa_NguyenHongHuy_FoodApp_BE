package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // Admin thấy hết
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Khách chỉ thấy Active
    public List<Product> getPublicProducts() {
        return productRepository.findByActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product createProduct(Product product) {
        // Mặc định khi tạo mới là hiện
        if (product.getActive() == null) product.setActive(true);
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updated) {
        Product product = getProductById(id);
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setPrice(updated.getPrice());
        product.setImageUrl(updated.getImageUrl());
        product.setStock(updated.getStock());
        product.setCategory(updated.getCategory());
        product.setCalories(updated.getCalories());
        product.setProtein(updated.getProtein());
        product.setCarbs(updated.getCarbs());
        product.setFat(updated.getFat());

        // Cho phép cập nhật trạng thái active
        if (updated.getActive() != null) {
            product.setActive(updated.getActive());
        }
        return productRepository.save(product);
    }

    // --- XÓA MỀM (SOFT DELETE) ---
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false); // Chỉ ẩn đi, không xóa database
        productRepository.save(product);
    }

    // Toggle nhanh (Dùng cho nút con mắt)
    public void toggleActive(Long id) {
        Product product = getProductById(id);
        product.setActive(!product.getActive());
        productRepository.save(product);
    }

    public Page<Product> search(Long categoryId, String q, int page, int limit, boolean isAdmin) {
        var pageable = PageRequest.of(Math.max(page-1,0), limit, Sort.by(Sort.Direction.DESC, "id"));
        if (isAdmin) {
            return productRepository.searchAdmin(categoryId, q, pageable);
        } else {
            return productRepository.searchPublic(categoryId, q, pageable);
        }
    }

    public List<Product> getTopSellingProducts(int limit) {
        List<Product> top = productRepository.findTopSelling(PageRequest.of(0, limit));

        if (top.isEmpty()) {
            return productRepository.findByActiveTrue().stream().limit(limit).toList();
        }
        return top;
    }
}