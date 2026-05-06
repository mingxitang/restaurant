package com.example.restaurant.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的 JWT 黑名单。
 * 适用于 WSL/Linux 生产环境或需要多实例共享黑名单的场景。
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisTokenBlacklist implements TokenBlacklist {

    private static final String PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String token, long expirationSeconds) {
        redisTemplate.opsForValue().set(PREFIX + token, "1", Duration.ofSeconds(expirationSeconds));
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
