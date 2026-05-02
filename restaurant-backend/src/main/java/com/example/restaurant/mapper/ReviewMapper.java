package com.example.restaurant.mapper;

import com.example.restaurant.entity.Review;

import java.util.List;

public interface ReviewMapper {
    int insert(Review review);

    List<Review> findAll();
}
