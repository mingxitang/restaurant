package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.entity.Dish;
import com.example.restaurant.service.DishService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/dishes")
public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public ApiResponse<List<Dish>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Integer categoryId) {
        return ApiResponse.ok(dishService.list(keyword, categoryId));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('管理员','服务员','厨师')")
    public ApiResponse<List<Dish>> lowStock(@RequestParam(required = false) Integer threshold) {
        return ApiResponse.ok(dishService.lowStock(threshold));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','厨师')")
    public ApiResponse<Dish> create(@RequestBody Dish dish) {
        return ApiResponse.ok(dishService.create(dish));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','厨师')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Dish dish) {
        dish.setDishId(id);
        dishService.update(dish);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('管理员','厨师')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('管理员','厨师')")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ApiResponse.ok(Map.of("url", ""));
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex);
        }
        String fileName = UUID.randomUUID() + ext;
        Path dir = Paths.get("uploads", "dishes").toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(fileName));
        return ApiResponse.ok(Map.of("url", "/uploads/dishes/" + fileName));
    }
}
