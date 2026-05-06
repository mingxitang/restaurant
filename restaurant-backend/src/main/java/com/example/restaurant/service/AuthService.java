package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.LoginRequest;
import com.example.restaurant.dto.LoginResponse;
import com.example.restaurant.entity.User;
import com.example.restaurant.mapper.UserMapper;
import com.example.restaurant.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByPhone(request.getPhone());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtTokenProvider.createToken(user.getUserId(), user.getPhone(), user.getRoleName());
        return new LoginResponse(token, user.getUserId(), user.getUsername(), user.getPhone(), user.getRoleName());
    }

    /** 计算 token 剩余有效秒数，用于登出时设置黑名单 TTL */
    public long getTokenRemainingSeconds(String token) {
        try {
            long expMillis = jwtTokenProvider.parseClaims(token).getExpiration().getTime();
            return Math.max(0, (expMillis - System.currentTimeMillis()) / 1000);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
