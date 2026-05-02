package com.example.restaurant.service;

import com.example.restaurant.entity.Dish;
import com.example.restaurant.mapper.DishMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishService {
    private final DishMapper dishMapper;

    public DishService(DishMapper dishMapper) {
        this.dishMapper = dishMapper;
    }

    public List<Dish> list(String keyword, Integer categoryId) {
        return dishMapper.findAll(keyword, categoryId);
    }

    public Dish create(Dish dish) {
        if (dish.getAvailable() == null) {
            dish.setAvailable(true);
        }
        dishMapper.insert(dish);
        return dish;
    }

    public void update(Dish dish) {
        dishMapper.update(dish);
    }

    public void delete(Long id) {
        dishMapper.delete(id);
    }

    public List<Dish> lowStock(Integer threshold) {
        return dishMapper.lowStock(threshold == null ? 10 : threshold);
    }
}
