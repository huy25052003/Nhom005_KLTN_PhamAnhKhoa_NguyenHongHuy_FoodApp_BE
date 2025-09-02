package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updated) {
        Product product = getProductById(id);
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setPrice(updated.getPrice());
        product.setImageUrl(updated.getImageUrl());
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public Page<Product> search(Long categoryId, String q, int page, int limit) {
        var pageable = PageRequest.of(Math.max(page-1,0), limit, Sort.by(Sort.Direction.DESC, "id"));
        return productRepository.search(categoryId, q, pageable);
    }
}
