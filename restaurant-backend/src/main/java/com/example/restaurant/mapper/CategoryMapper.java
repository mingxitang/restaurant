package com.example.restaurant.mapper;

import com.example.restaurant.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CategoryMapper {
    List<Category> findAll();

    Category findById(@Param("categoryId") Integer categoryId);

    int insert(Category category);

    int update(Category category);

    int delete(@Param("categoryId") Integer categoryId);
}
