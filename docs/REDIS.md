# Redis 集成方案

## 设计思路

Redis 是可选的增强组件，**不是必须依赖**。通过一个开关 `app.redis.enabled` 控制是否启用：

| 环境 | `app.redis.enabled` | 黑名单实现 | 缓存实现 |
|------|---------------------|-----------|---------|
| Windows（默认） | `false` | `InMemoryTokenBlacklist`（ConcurrentHashMap） | Spring 默认 ConcurrentMapCache |
| WSL / Linux | `true` | `RedisTokenBlacklist`（Redis） | RedisCacheManager |

这样保证项目在 Windows 上开箱即用，在 WSL/Linux 上获得 Redis 的全部优势。

## 架构

```
TokenBlacklist（接口）
├── InMemoryTokenBlacklist    ← app.redis.enabled = false（默认）
└── RedisTokenBlacklist       ← app.redis.enabled = true

RedisConfig                   ← app.redis.enabled = true 时创建 CacheManager
```

两个实现通过 `@ConditionalOnProperty` 互斥，**同一时间只有一个生效**。

## 功能场景

### 1. JWT 登出失效（黑名单）

**问题**：JWT 是无状态的，用户登出后 token 在有效期内仍然可以访问。

**方案**：登出时将 token 加入黑名单，过期时间设为其剩余有效期。

- `POST /api/auth/logout` → 解析 token 剩余有效期 → 加入黑名单
- `JwtAuthenticationFilter` → 每个请求先查黑名单 → 被拉黑的 token 直接拒绝

### 2. 缓存（启用 Redis 时生效）

`RedisConfig` 使用 `@EnableCaching` 和 `RedisCacheManager`，后续可以在 Service 层用 `@Cacheable` / `@CacheEvict` 缓存菜品、分类等读多写少的数据。

## Windows 使用（默认）

无需任何配置，直接启动即可。使用内存黑名单。

**限制**：
- 服务重启后黑名单丢失（JWT 本身有过期时间，影响可控）
- 不支持多实例部署共享黑名单
- 缓存为 JVM 内存级别

## WSL / Linux 启用 Redis

### 1. 确认 Redis 已启动

```bash
redis-cli ping
# 应返回 PONG
```

### 2. 修改 `application.yml`

**三步改动**：

```yaml
# ① 删除 Redis 自动配置的排除项（删除下面 3 行）
#  autoconfigure:
#    exclude:
#      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

# ② 启用 Redis
app:
  redis:
    enabled: true   # 改为 true

# ③ 取消 Redis 连接配置的注释
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### 3. 重启后端

```bash
cd restaurant-backend
mvn spring-boot:run
```

启动日志中出现 `LettuceConnectionFactory` 即表示 Redis 已连接。

## 涉及的源码文件

| 文件 | 说明 |
|------|------|
| `service/TokenBlacklist.java` | 黑名单接口 |
| `service/InMemoryTokenBlacklist.java` | 内存实现，默认启用 |
| `service/RedisTokenBlacklist.java` | Redis 实现，`app.redis.enabled=true` 时启用 |
| `config/RedisConfig.java` | Redis CacheManager 配置 |
| `security/JwtAuthenticationFilter.java` | 增加黑名单校验 |
| `controller/AuthController.java` | 新增 `/api/auth/logout` 接口 |
| `service/AuthService.java` | 新增 `getTokenRemainingSeconds()` 方法 |
| `RestaurantApplication.java` | 新增 `@EnableScheduling`（定时清理内存黑名单） |

## 注意事项

1. **Redis key 命名**：`jwt:blacklist:{token}`，统一前缀避免冲突
2. **内存黑名单清理**：每 10 分钟定时清理过期条目，`isBlacklisted()` 也会惰性清理
3. **token 过期后**：黑名单条目自动失效（Redis TTL / 内存清理），不会无限膨胀
4. **反结账不涉及黑名单**：黑名单只管理登出，不影响业务操作
