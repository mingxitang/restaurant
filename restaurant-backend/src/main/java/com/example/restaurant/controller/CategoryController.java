package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.entity.Category;
import com.example.restaurant.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(categoryService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<Category> create(@RequestBody Category category) {
        return ApiResponse.ok(categoryService.create(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<Void> update(@PathVariable Integer id, @RequestBody Category category) {
        category.setCategoryId(id);
        categoryService.update(category);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
