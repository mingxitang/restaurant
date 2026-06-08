package com.example.restaurant.mapper;

import com.example.restaurant.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    User findByPhone(@Param("phone") String phone);

    User findByWxOpenid(@Param("wxOpenid") String wxOpenid);

    User findById(@Param("userId") Long userId);

    Integer findRoleIdByName(@Param("roleName") String roleName);

    List<User> findAll(@Param("keyword") String keyword);

    List<User> findPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    long countAll(@Param("keyword") String keyword);

    int insert(User user);

    int update(User user);
}
