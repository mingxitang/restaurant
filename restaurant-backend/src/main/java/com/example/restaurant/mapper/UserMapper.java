package com.example.restaurant.mapper;

import com.example.restaurant.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User findByPhone(@Param("phone") String phone);

    User findById(@Param("userId") Long userId);

    List<User> findAll(@Param("keyword") String keyword);

    int insert(User user);

    int update(User user);
}
