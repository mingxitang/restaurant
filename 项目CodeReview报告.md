# 餐厅点餐系统 — Code Review 报告

> **审查日期**：2026-05-25  
> **项目**：restaurant（餐厅点餐系统）  
> **技术栈**：Spring Boot 3.3.5 / Vue 3 / MyBatis / WeChat 小程序  

---

## 总览

| 维度 | 评级 | 说明 |
|------|------|------|
| 项目结构 | ✅ 良好 | 三层分离清晰：backend / admin / customer / miniprogram |
| 代码规范 | ⚠️ 一般 | 整体风格一致，但部分细节粗糙 |
| 安全性 | 🔴 有隐患 | 多个安全漏洞需要修复 |
| 健壮性 | ⚠️ 一般 | 缺少校验、无测试、错误处理不足 |
| 可维护性 | ⚠️ 一般 | 巨型组件、无文档、无测试 |
| 完整度 | ⚠️ 一般 | 核心功能可用，但缺测试/部署/Docker |

---

## 🔴 严重问题（必须修复）

### 1. 零测试覆盖

`src/test` 目录为空，整个项目没有任何单元测试或集成测试。

```
restaurant-backend/src/test/   ← 空的！
```

对于一个包含下单、支付、退款、库存管理的业务系统，没有测试意味着每次改动都可能引入回归 bug。

**建议**：
- 至少为核心 Service（OrderService、RefundService）编写单元测试
- 使用 `@SpringBootTest` + H2 内存数据库编写 API 集成测试
- 退款逻辑（库存恢复 + 金额扣减 + 订单明细删除）是 Bug 高风险区，必须覆盖

---

### 2. JWT 密钥硬编码且提示"请修改"

`application.yml` 中 JWT secret 明晃晃地写着 `change-me`：

```yaml
jwt:
  secret: "restaurant-course-design-secret-key-change-me-please-2026"
  expiration-minutes: 1440
```

任何拿到这份代码的人都能伪造 JWT Token，以任意身份登录系统。

**建议**：
- 通过环境变量注入：`${JWT_SECRET}`，本地开发用 `.env`
- 密钥长度至少 256 位（当前字符串不够安全）
- 已在 ROADMAP Phase 6 标记为 TODO，应尽快完成

---

### 3. 订单状态修改接口无权限控制

`OrderController.updateStatus()` 没有任何 `@PreAuthorize`：

```java
// ❌ 危险：任何登录用户都能改
@PutMapping("/{id}/status")
public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
    orderService.updateStatus(id, body.get("status"));
    return ApiResponse.ok();
}
```

顾客可以通过 `/api/orders/1/status` 把自己的订单改成 "PAID" 然后直接走人。

**建议**：加上 `@PreAuthorize("hasAnyRole('管理员','服务员')")`

---

### 4. 数据库密码明文硬编码

```yaml
spring:
  datasource:
    username: root
    password: 1234   # ← 明文 + 弱密码
```

**建议**：使用 `${DB_PASSWORD}` 环境变量，生产环境用强密码且非 root 账户。

---

### 5. 全局异常处理器泄露内部错误信息

```java
@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public ApiResponse<Void> handleException(Exception ex) {
    return ApiResponse.fail(ex.getMessage());  // ← 直接把异常消息返回给前端
}
```

生产环境中这会把 SQL 错误、堆栈信息等敏感内容暴露给客户端。

**建议**：500 错误统一返回 `"系统繁忙，请稍后再试"`，详细信息只输出到日志。

---

## 🟠 中等问题（建议修复）

### 6. DashboardView.vue 过于臃肿（37KB）

`restaurant-admin/src/views/DashboardView.vue` 高达 37KB，包含：
- 桌台管理与桌台编辑
- 订单列表与订单详情
- 菜品搜索与加菜
- 二维码生成
- 服务员呼叫处理
- 订单转移 / 结账 / 退款

一个文件承担了太多职责，极难维护。

**建议**：拆分为独立组件：
- `TableGrid.vue` — 桌台网格
- `TableDetailModal.vue` — 桌台详情弹窗（订单 + 加菜）
- `QrCodeModal.vue` — 二维码弹窗
- `WaiterCallPanel.vue` — 服务员呼叫面板

---

### 7. 所有请求 DTO 缺少输入校验

没有一个 DTO 使用了 `@Valid` / `@NotBlank` / `@NotNull` 等 Jakarta Validation 注解：

```java
@Data
public class OrderCreateRequest {
    private Integer tableId;      // 没有 @NotNull
    private Long userId;          // 没有 @NotNull
    private List<OrderItemRequest> items;  // 没有 @NotEmpty
}
```

这意味着顾客可以提交空菜单、空桌号的订单，在 Service 层才会报错。

**建议**：给所有 DTO 添加校验注解，Controller 参数加上 `@Valid`。

---

### 8. JWT Token 存储在 localStorage（XSS 风险）

前端将 JWT 存在 `localStorage`：

```javascript
// admin/src/api/http.js & customer/src/api/http.js
localStorage.setItem('token', ...)
```

通过 JavaScript 可读，一旦存在 XSS 漏洞，Token 直接泄露。

**建议**：改用 `httpOnly` Cookie 存储，或至少使用 `sessionStorage` + 较短的 Token 有效期。前端路由守卫也需要改成从 Cookie 读取。

---

### 9. 顾客端接口缺少角色隔离

`CustomerController` 类级别没有 `@PreAuthorize`，所有方法只依赖 `SecurityConfig` 的 `.anyRequest().authenticated()`：

```java
@RestController
@RequestMapping("/api/customer")
// ❌ 缺少 @PreAuthorize("hasRole('顾客')")
public class CustomerController { ... }
```

这意味着管理员/服务员/厨师的 Token 也能访问顾客端的下单、支付接口。虽然可能不是严重的安全漏洞（毕竟是内部操作），但违反了最小权限原则。

**建议**：加上 `@PreAuthorize("hasRole('顾客')")`

---

### 10. 退款逻辑存在冗余操作（潜在 Bug）

`RefundService.create()` 中对订单明细做了两个操作：

```java
orderMapper.deleteRefundedDetail(request.getOrderId(), request.getDishId(), request.getQuantity());
orderMapper.decreaseDetailQuantity(request.getOrderId(), request.getDishId(), request.getQuantity());
```

看方法名，一个"删除"一个"减少数量"，同时调用可能导致：
- 如果 `deleteRefundedDetail` 已经删除了记录，`decreaseDetailQuantity` 的 UPDATE 会返回 0 行
- 如果 `decreaseDetailQuantity` 先执行并减到 0，再 `deleteRefundedDetail` 会删 0 行

**建议**：确认这两步的语义。要么只保留一个，要么明确"先减再删"的逻辑并加注释。

---

### 11. 内存 Token 黑名单重启即丢失

```java
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenBlacklist implements TokenBlacklist {
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();
```

默认模式下（Windows），Token 黑名单存在 JVM 内存中。服务重启后，所有已登出但未过期的 Token 重新变成有效 Token。

**建议**：
- 生产环境务必启用 Redis 模式
- 或改用短期 Token + Refresh Token 方案，避免需要黑名单

---

### 12. 列表接口缺少分页

所有 `list` 接口返回全量数据：

```java
@GetMapping
public ApiResponse<List<Order>> list(...) {
    return ApiResponse.ok(orderService.list(status, startDate, endDate));
}
```

当菜品/订单/用户数据量增长后，一次性返回全量会造成性能问题。

**建议**：添加分页参数（`page`、`size`），使用 MyBatis PageHelper 或手动 LIMIT/OFFSET。

---

## 🟡 低优先级（可以改进）

### 13. 无 API 文档

项目中没有任何 Swagger/OpenAPI 注解。前端对接靠看 Controller 代码。

**建议**：引入 `springdoc-openapi`，给 Controller 加注解。

---

### 14. MenuResponse 风格不统一

其他 DTO 都用 Lombok `@Data`，唯独 `MenuResponse` 手动写 getter/setter：

```java
// 用 Lombok 多好，20 行代码缩成 5 行
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuResponse {
    private List<Category> categories;
    private List<Dish> dishes;
}
```

---

### 15. styles.css 缺乏组织

- `admin/src/styles.css` — 21.6KB
- `customer/src/styles.css` — 12.7KB

纯手写 CSS，没有使用 CSS 变量、Scoped Style、CSS Modules 或 Tailwind 等任何工具。

**建议**：至少用 Scoped Style + CSS 变量来组织。

---

### 16. callWaiter 接口参数不优雅

```java
@PostMapping("/call-waiter")
public ApiResponse<Void> callWaiter(@RequestBody java.util.Map<String, Object> body) {
    // 手动解析 Map...
    Object tableIdValue = body.get("tableId");
    Integer tableId = tableIdValue == null ? null : Integer.valueOf(String.valueOf(tableIdValue));
```

用 `Map<String, Object>` 接收参数然后手动解析，建议定义一个 `WaiterCallRequest` DTO。

---

### 17. reportStartDate 返回 null 有 NPE 风险

```java
private LocalDate reportStartDate(String period) {
    // ...if/else 链...
    return null;  // ← 当 period 不是 day/week/month 时返回 null
}
```

如果前端传入未知 period，SQL 中 `<if test="startDate != null">` 不会报错但查询结果可能不符合预期（查了所有时间段）。

**建议**：不合法参数直接抛 `BusinessException`，而不是静默返回 null。

---

### 18. CORS 配置过于宽松

```java
.allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
.allowedHeaders("*")
.allowCredentials(true)
```

`allowedHeaders("*")` + `allowCredentials(true)` 是 CORS 反模式，浏览器会拒绝这种组合。虽然开发环境够用，但生产部署时需要注意。

---

### 19. application.yml 中 MySQL URL 无连接池参数

```yaml
url: jdbc:mysql://localhost:3306/restaurant_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

缺少 HikariCP 连接池相关参数（最大连接数、超时时间等），在高并发场景可能不够。

---

## 📋 ROADMAP 未完成项

从 `ROADMAP.md` Phase 6-7：

| 任务 | 状态 | 优先级 |
|------|------|--------|
| JWT 密钥环境变量化 | ❌ | 🔴 高 |
| 登录限流（rate limiting） | ❌ | 🟠 中 |
| 单元测试 | ❌ | 🔴 高 |
| API 测试清单 | ❌ | 🟠 中 |
| 部署文档 | ❌ | 🟡 低 |
| Docker Compose | ❌ | 🟡 低 |

---

## ✅ 做得好的地方

1. **项目结构**：前后端分离 + 小程序三端分离，目录层次清晰
2. **分层架构**：Controller → Service → Mapper，职责明确
3. **构造器注入**：全部使用构造器注入而非 `@Autowired` 字段注入
4. **JWT 无状态认证**：`STATELESS` session 策略 + 黑名单机制
5. **Redis 可选开关**：`app.redis.enabled` 条件配置，Windows 环境默认无需 Redis 即可启动
6. **MyBatis XML**：动态 SQL 使用得当，复杂查询放在 XML 而非 Java 代码拼接
7. **统一的 API 响应格式**：`ApiResponse<T>` 包装器，前后端对接一致
8. **数据库索引**：关键字段（status、order_time、refund_time、call_time）有索引
9. **视图**：`v_kitchen_queue`、`v_monthly_revenue_trend` 等视图简化了查询逻辑
10. **README 完善**：有运行步骤、默认账号、功能矩阵表

---

## 修复优先级建议

```
第一轮（本周）：
  ✅ 修复 OrderController.updateStatus 缺少权限
  ✅ 修复 GlobalExceptionHandler 500 错误信息泄露
  ✅ 添加核心 Service 的单元测试

第二轮（下周）：
  ✅ JWT 密钥环境变量化
  ✅ 数据库密码环境变量化
  ✅ DTO 参数校验
  ✅ 登录限流

第三轮（后续）：
  ✅ DashboardView 拆分
  ✅ 分页
  ✅ Swagger
  ✅ Docker Compose
```
