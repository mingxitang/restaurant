package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.LoginRequest;
import com.example.restaurant.dto.LoginResponse;
import com.example.restaurant.dto.WxLoginRequest;
import com.example.restaurant.service.AuthService;
import com.example.restaurant.service.LoginRateLimiter;
import com.example.restaurant.service.TokenBlacklist;
import com.example.restaurant.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final TokenBlacklist tokenBlacklist;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, TokenBlacklist tokenBlacklist, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.tokenBlacklist = tokenBlacklist;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String rateLimitKey = loginKey(request.getPhone(), servletRequest);
        loginRateLimiter.assertAllowed(rateLimitKey);
        try {
            LoginResponse response = authService.login(request);
            loginRateLimiter.clear(rateLimitKey);
            return ApiResponse.ok(response);
        } catch (BusinessException ex) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw ex;
        }
    }

    @PostMapping("/wx-login")
    public ApiResponse<LoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.ok(authService.wxLogin(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        // 解析剩余有效期，加入黑名单
        long remainingSeconds = authService.getTokenRemainingSeconds(token);
        if (remainingSeconds > 0) {
            tokenBlacklist.blacklist(token, remainingSeconds);
        }
        return ApiResponse.ok(null);
    }

    private String loginKey(String phone, HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        return clientIp + ":" + phone;
    }
}
