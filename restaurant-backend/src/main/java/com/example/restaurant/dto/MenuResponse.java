package com.example.restaurant.dto;

import com.example.restaurant.entity.Category;
import com.example.restaurant.entity.Dish;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuResponse {
    private List<Category> categories;
    private List<Dish> dishes;
}
