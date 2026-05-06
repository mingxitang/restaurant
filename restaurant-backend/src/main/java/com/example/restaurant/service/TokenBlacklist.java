package com.example.restaurant.service;

/**
 * JWT 黑名单接口。登出后将 token 加入黑名单，过滤器校验时先查黑名单。
 * 两套实现：Redis 版（WSL/Linux）和内存版（Windows 无 Redis）。
 */
public interface TokenBlacklist {

    /**
     * 将 token 加入黑名单
     * @param token JWT 原始字符串
     * @param expirationSeconds 剩余有效秒数（取 token 自身过期时间）
     */
    void blacklist(String token, long expirationSeconds);

    /**
     * 检查 token 是否在黑名单中
     */
    boolean isBlacklisted(String token);
}
