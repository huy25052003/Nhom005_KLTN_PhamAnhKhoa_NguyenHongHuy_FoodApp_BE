package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Category;
import org.example.server.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public ResponseEntity<List<Category>> getAll() { return ResponseEntity.ok(service.getAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getOne(@PathVariable Long id) { return ResponseEntity.ok(service.getOne(id)); }

    @PostMapping
    public ResponseEntity<Category> create(@RequestBody Category c) { return ResponseEntity.ok(service.create(c)); }

    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody Category c) {
        return ResponseEntity.ok(service.update(id, c));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
