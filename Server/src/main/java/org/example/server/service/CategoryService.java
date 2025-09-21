package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Category;
import org.example.server.repository.CategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepo;

    public List<Category> getAll() { return categoryRepo.findAll(); }

    public Category getOne(Long id) {
        return categoryRepo.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
    }

    public Category create(Category c) {
        if (c.getName() == null || c.getName().isBlank()) throw new RuntimeException("Name is required");
        if (categoryRepo.existsByNameIgnoreCase(c.getName())) throw new RuntimeException("Category already exists");
        return categoryRepo.save(c);
    }

    public Category update(Long id, Category c) {
        Category e = getOne(id);
        if (c.getName()!=null && !c.getName().isBlank()) {
            if (!e.getName().equalsIgnoreCase(c.getName()) && categoryRepo.existsByNameIgnoreCase(c.getName()))
                throw new RuntimeException("Category already exists");
            e.setName(c.getName());
        }
        if (c.getDescription()!=null) e.setDescription(c.getDescription());
        return categoryRepo.save(e);
    }

    public void delete(Long id) {
        try { categoryRepo.deleteById(id); }
        catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Không thể xoá: danh mục đang được sử dụng");
        }
    }
}
