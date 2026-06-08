package com.example.restaurant.service;

import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.entity.User;
import com.example.restaurant.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> list(String keyword) {
        return userMapper.findAll(keyword);
    }

    public PageResponse<User> page(String keyword, Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = userMapper.countAll(keyword);
        List<User> records = total == 0 ? List.of() : userMapper.findPage(keyword, params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
    }

    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getEnabled() == null) {
            user.setEnabled(true);
        }
        userMapper.insert(user);
        return user;
    }

    public void update(User user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userMapper.update(user);
    }
}
