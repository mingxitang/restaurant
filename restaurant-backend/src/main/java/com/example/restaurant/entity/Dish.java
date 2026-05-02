package com.example.restaurant.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Dish {
    private Long dishId;
    private String dishName;
    private BigDecimal price;
    private Integer stock;
    private String image;
    private String description;
    private Integer categoryId;
    private String categoryName;
    private Boolean available;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
