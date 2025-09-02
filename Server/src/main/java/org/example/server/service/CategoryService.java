package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Category;
import org.example.server.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepo;
    public List<Category> list() {
        return categoryRepo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
}
