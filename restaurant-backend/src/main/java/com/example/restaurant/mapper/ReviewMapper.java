package com.example.restaurant.mapper;

import com.example.restaurant.entity.Review;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewMapper {
    int insert(Review review);

    int countByOrderId(@Param("orderId") Long orderId);

    List<Review> findAll();

    List<Review> findPage(@Param("offset") int offset, @Param("size") int size);

    long countAll();
}
