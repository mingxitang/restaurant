package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.entity.User;
import com.example.restaurant.mapper.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserMapper userMapper;

    public CurrentUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("请先登录");
        }
        String phone = authentication.getName();
        User user = userMapper.findByPhone(phone);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("当前登录用户不存在或已禁用");
        }
        return user;
    }
}
