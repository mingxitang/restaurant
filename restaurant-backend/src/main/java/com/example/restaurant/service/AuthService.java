package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.LoginRequest;
import com.example.restaurant.dto.LoginResponse;
import com.example.restaurant.dto.WxLoginRequest;
import com.example.restaurant.entity.User;
import com.example.restaurant.mapper.UserMapper;
import com.example.restaurant.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient restClient;
    private final String wxAppId;
    private final String wxSecret;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       @Value("${app.wechat.app-id:}") String wxAppId,
                       @Value("${app.wechat.secret:}") String wxSecret) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restClient = RestClient.create();
        this.wxAppId = wxAppId;
        this.wxSecret = wxSecret;
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

    public LoginResponse wxLogin(WxLoginRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BusinessException("微信登录 code 不能为空");
        }
        if (wxAppId == null || wxAppId.isBlank() || wxSecret == null || wxSecret.isBlank()) {
            throw new BusinessException("后端未配置微信小程序 appId/secret");
        }

        String openid = fetchOpenid(request.getCode());
        User user = userMapper.findByWxOpenid(openid);
        if (user == null) {
            user = createWxCustomer(openid, request.getNickName());
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtTokenProvider.createToken(user.getUserId(), user.getPhone(), user.getRoleName());
        return new LoginResponse(token, user.getUserId(), user.getUsername(), user.getPhone(), user.getRoleName());
    }

    @SuppressWarnings("unchecked")
    private String fetchOpenid(String code) {
        Map<String, Object> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.weixin.qq.com")
                        .path("/sns/jscode2session")
                        .queryParam("appid", wxAppId)
                        .queryParam("secret", wxSecret)
                        .queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build())
                .retrieve()
                .body(Map.class);

        if (result == null) {
            throw new BusinessException("微信登录失败：微信接口无响应");
        }
        Object errCode = result.get("errcode");
        if (errCode != null && !"0".equals(String.valueOf(errCode))) {
            throw new BusinessException("微信登录失败：" + result.getOrDefault("errmsg", "code 无效"));
        }
        Object openid = result.get("openid");
        if (openid == null || String.valueOf(openid).isBlank()) {
            throw new BusinessException("微信登录失败：未获取到 openid");
        }
        return String.valueOf(openid);
    }

    private User createWxCustomer(String openid, String nickName) {
        Integer customerRoleId = userMapper.findRoleIdByName("顾客");
        if (customerRoleId == null) {
            throw new BusinessException("顾客角色不存在，请先初始化角色数据");
        }
        User user = new User();
        user.setUsername(nickName == null || nickName.isBlank() ? "微信顾客" : nickName);
        user.setPhone(buildWxPhone(openid));
        user.setWxOpenid(openid);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRoleId(customerRoleId);
        user.setEnabled(true);
        user.setPoints(0);
        userMapper.insert(user);
        return userMapper.findByWxOpenid(openid);
    }

    private String buildWxPhone(String openid) {
        return "wx" + Integer.toUnsignedString(openid.hashCode());
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
