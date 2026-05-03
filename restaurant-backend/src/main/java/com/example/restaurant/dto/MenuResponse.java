package com.example.restaurant.dto;

import com.example.restaurant.entity.Category;
import com.example.restaurant.entity.Dish;

import java.util.List;

public class MenuResponse {
    private List<Category> categories;
    private List<Dish> dishes;

    public MenuResponse(List<Category> categories, List<Dish> dishes) {
        this.categories = categories;
        this.dishes = dishes;
    }

    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public List<Dish> getDishes() { return dishes; }
    public void setDishes(List<Dish> dishes) { this.dishes = dishes; }
}
