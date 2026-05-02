package com.example.restaurant.service;

import com.example.restaurant.entity.Category;
import com.example.restaurant.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> list() {
        return categoryMapper.findAll();
    }

    public Category create(Category category) {
        categoryMapper.insert(category);
        return category;
    }

    public void update(Category category) {
        categoryMapper.update(category);
    }

    public void delete(Integer id) {
        categoryMapper.delete(id);
    }
}
