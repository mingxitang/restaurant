package com.example.restaurant.mapper;

import com.example.restaurant.entity.Dish;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DishMapper {
    List<Dish> findAll(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId);

    List<Dish> findPage(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId,
                        @Param("offset") int offset, @Param("size") int size);

    long countAll(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId);

    Dish findById(@Param("dishId") Long dishId);

    int insert(Dish dish);

    int update(Dish dish);

    int delete(@Param("dishId") Long dishId);

    int decreaseStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    int increaseStock(@Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    List<Dish> lowStock(@Param("threshold") Integer threshold);
}
