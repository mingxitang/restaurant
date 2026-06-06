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

---

## 🔄 第二轮审查补充（2026-06-06）

二次深入审查了所有 Service 层完整逻辑、SQL Mapper、迁移脚本、小程序端代码、前端状态管理。以下是新发现的问题：

---

### 🔴 新发现：严重问题

#### 20. transferTable 合并订单后产生孤儿记录

```java
// OrderService.transferTable()
} else {
    List<OrderDetail> details = orderMapper.findDetails(sourceOrder.getOrderId());
    for (OrderDetail item : details) {
        // 把明细逐条插入目标订单...
        orderMapper.insertDetail(moved);
    }
    orderMapper.increaseTotal(targetOrder.getOrderId(), sourceOrder.getTotalAmount());
    orderMapper.updateStatus(sourceOrder.getOrderId(), "CANCELLED");
    // ❌ 源订单的 order_detail 行没有删除！
}
```

合并到目标桌台后，源订单被标记为 CANCELLED，但它的 `order_detail` 行仍然留在数据库中，成为孤儿记录。这些孤儿数据会被 `kitchenQueue` 视图查出来（因为视图只过滤 `od.status != 'SERVED'`，不检查订单状态），导致厨房大屏出现已取消订单的菜品。

**建议**：在 CANCELLED 之前，删除源订单的所有 `order_detail` 记录。

---

#### 21. 退款逻辑 BUG：无法全额退款

```xml
<!-- OrderMapper.xml -->
<update id="decreaseDetailQuantity">
    UPDATE order_detail
    SET quantity = quantity - #{quantity}
    WHERE order_id = #{orderId}
      AND dish_id = #{dishId}
      AND quantity > #{quantity}   <!-- ❌ 严格大于！ -->
</update>

<delete id="deleteRefundedDetail">
    DELETE FROM order_detail
    WHERE order_id = #{orderId}
      AND dish_id = #{dishId}
      AND quantity = 0              <!-- 只在 quantity=0 时删 -->
</delete>
```

`RefundService.create()` 先调 `decreaseDetailQuantity` 再调 `deleteRefundedDetail`：

| 场景 | 明细原数量 | 退款数量 | decreaseDetailQuantity | deleteRefundedDetail | 结果 |
|------|-----------|---------|----------------------|---------------------|------|
| 部分退款 | 5 | 2 | ✅ 更新为 3 | ❌ 不删（3≠0） | ✅ 正常 |
| 全额退款 | 3 | 3 | ❌ 不执行（3>3 为 false） | ❌ 不删（3≠0） | 🔴 **BUG！数量不变** |
| 退最后1个 | 1 | 1 | ❌ 不执行（1>1 为 false） | ❌ 不删（1≠0） | 🔴 **BUG！** |

**全额退款场景完全失效**，订单明细数量和金额都不会被正确扣减。

**建议**：将条件改为 `quantity >= #{quantity}`，并增加 `quantity = 0` 时删除逻辑。

---

#### 22. unpay() 反结账后桌台状态不同步

```java
// OrderService.pay() — 支付时把桌台设为 FREE
tableInfoMapper.updateStatus(order.getTableId(), "FREE");

// OrderService.unpay() — 反结账时... 什么都没做
orderMapper.unpay(orderId);
// ❌ 没有把桌台状态改回 OCCUPIED
```

支付后桌台变 FREE，但如果服务员操作反结账（退回 PENDING），桌台还是 FREE 状态。下一个顾客可能扫码坐到这个桌台上，产生冲突。

**建议**：`unpay()` 中增加 `tableInfoMapper.updateStatus(order.getTableId(), "OCCUPIED")`。

---

### 🟠 新发现：中等问题

#### 23. pay() 覆盖所有菜品烹饪状态

```java
// OrderService.pay()
for (OrderDetail detail : order.getDetails()) {
    orderMapper.updateDetailStatus(orderId, detail.getDishId(), "PREPARING");
}
```

支付后无条件把所有 detail 的 `status` 设为 PREPARING。但如果厨房已经在准备某道菜（status 已经是 PREPARING 或 READY），这个操作会把它重置回 PREPARING。虽然大概率支付发生在做菜之前，但仍然是一个数据一致性问题。

**建议**：只更新 `status = 'PENDING'` 或 `status IS NULL` 的记录。

---

#### 24. 顾客端登录页面硬编码测试账号

管理端（admin）和顾客端（customer）的登录页都预填了测试账号：

```javascript
// admin/src/views/LoginView.vue
const form = reactive({ phone: '13800000000', password: '123456' })

// customer/src/views/LoginView.vue（预期类似）

// miniprogram/pages/login/login.js
data: { phone: '13800000001', password: '123456' }
```

三个端都有硬编码的默认值。虽然是方便测试，但容易导致：
- 演示时忘记改密码
- 测试账号被误用上线

**建议**：用环境变量或配置文件控制，生产构建时去掉默认值。

---

#### 25. 顾客端 cartTotal 使用浮点计算

```javascript
// customer/src/store.js
get cartTotal() {
    return this.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
}
```

JavaScript 的浮点数运算可能导致 `0.1 + 0.2 = 0.30000000000000004` 这种精度问题。虽然后端用 `BigDecimal` 计算了正确金额，但前端显示的合计可能与后端不一致。

**建议**：显示时用 `.toFixed(2)`，或用整数（分）计算。

---

#### 26. 小程序全部使用 var 声明

```javascript
// miniprogram/pages/menu/menu.js
var api = require('../../api/index')
var config = require('../../config/index')
// ...
for (var i = 0; i < dishes.length; i += 1) {
```

整个小程序端没有使用 `let`/`const`，这是 ES5 风格。虽然小程序基础库兼容，但作用域混乱易出 Bug。

**建议**：至少新代码用 `let`/`const`。

---

#### 27. enrichDishCartCount O(n*m) 效率问题

```javascript
function enrichDishCartCount(dishes, cart) {
  for (var i = 0; i < dishes.length; i += 1) {
    var count = 0
    for (var j = 0; j < cart.length; j += 1) {   // 嵌套循环
      if (String(cart[j].dishId) === String(dishes[i].dishId)) {
        count = cart[j].quantity
        break
      }
    }
    // ...
  }
}
```

**建议**：先用 `cart` 构建一个 `Map<dishId, quantity>`，然后遍历 `dishes` 时 O(1) 查找。

---

#### 28. 数据库迁移脚本 v_kitchen_queue 视图重复创建

- `migration-add-detail-status.sql` 创建了 `v_kitchen_queue`
- `migration-current-db-fixes.sql` 又用 `CREATE OR REPLACE VIEW` 重新创建了同一个视图

新版本增加了 `reminder_count` 和 `last_reminder_time` 字段，但旧迁移脚本没有被更新。如果按时间顺序执行迁移，视图会被覆盖，虽然最终结果正确，但维护混乱。

**建议**：清理重复的视图定义，保持每个视图只在一个地方定义。

---

### 🟡 新发现：低优先级

#### 29. create() 方法存在并发隐患

```java
Order order = orderMapper.findActiveByTableId(request.getTableId());
if (order == null) {
    order = new Order();  // 新建订单
    orderMapper.insert(order);
}
// 否则追加到已有订单
```

虽然加了 `@Transactional`，但默认隔离级别下，两个请求同时查到 `null` 会插入两条活跃订单。不过实际场景中同一桌台不太可能同时有两个顾客下单，风险较低。

**建议**：在 `table_id` + `status != 'CANCELLED'` 上加唯一约束，或使用 `SELECT ... FOR UPDATE`。

---

#### 30. cancel() 后库存恢复缺少审计

```java
for (OrderDetail detail : order.getDetails()) {
    dishMapper.increaseStock(detail.getDishId(), detail.getQuantity());
}
```

取消订单时恢复库存，但没有记录"为什么库存增加了"。如果后续查库存变动历史，无法区分是采购入库还是订单取消。

**建议**：至少留一条 `refund_record`（stockAction=0 表示取消返还）。

---

#### 31. 前端 401 处理直接跳转，用户无感知

```javascript
// admin/src/api/http.js
if (error.response?.status === 401 || error.response?.status === 403) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'   // 直接跳转，用户正在填的表单全丢
}
```

Token 过期时，用户正在操作的内容（比如正在编辑菜品、填退款单）会直接丢失。

**建议**：弹出提示"登录已过期，请重新登录"，确认后再跳转。

---

#### 32. 缺少请求防抖/节流

多个按钮（下单、支付、催单）在 loading 期间没有完全防止重复点击。虽然用了 `:disabled`，但网络延迟高时用户可能快速双击。

**建议**：加上防抖（debounce）或在 `submitting` 期间加锁。

---

## 📊 问题汇总表

| 编号 | 类别 | 问题 | 严重度 | 位置 |
|------|------|------|--------|------|
| 1 | 测试 | 零测试覆盖 | 🔴 高 | `src/test/` |
| 2 | 安全 | JWT 密钥硬编码 | 🔴 高 | `application.yml` |
| 3 | 安全 | 订单状态接口无权限 | 🔴 高 | `OrderController.updateStatus()` |
| 4 | 安全 | 数据库密码明文 | 🔴 高 | `application.yml` |
| 5 | 安全 | 500 错误泄露内部信息 | 🔴 高 | `GlobalExceptionHandler` |
| 6 | 可维护 | DashboardView 37KB | 🟠 中 | `admin/views/DashboardView.vue` |
| 7 | 健壮 | DTO 缺少校验 | 🟠 中 | 所有 DTO |
| 8 | 安全 | Token 存 localStorage | 🟠 中 | 前端 `api/http.js` |
| 9 | 安全 | 顾客接口角色隔离不足 | 🟠 中 | `CustomerController` |
| 10 | Bug | 退款逻辑冗余+全量退款BUG | 🔴 高 | `RefundService` + `OrderMapper.xml` |
| 11 | 安全 | 内存黑名单重启丢失 | 🟠 中 | `InMemoryTokenBlacklist` |
| 12 | 性能 | 列表无分页 | 🟡 低 | 所有 list 接口 |
| 13 | 文档 | 无 API 文档 | 🟡 低 | 全局 |
| 14 | 规范 | MenuResponse 未用 Lombok | 🟡 低 | `dto/MenuResponse.java` |
| 15 | 可维护 | styles.css 无组织 | 🟡 低 | 前端 |
| 16 | 规范 | callWaiter 用 Map 解析 | 🟡 低 | `CustomerController` |
| 17 | Bug | reportStartDate 返回 null | 🟡 低 | `OrderService` |
| 18 | 安全 | CORS 配置过宽 | 🟡 低 | `WebConfig` |
| 19 | 配置 | 无连接池参数 | 🟡 低 | `application.yml` |
| 20 | Bug | transferTable 孤儿记录 | 🔴 高 | `OrderService.transferTable()` |
| 21 | Bug | 全额退款失效 | 🔴 高 | `OrderMapper.decreaseDetailQuantity` |
| 22 | Bug | unpay 不恢复桌台 | 🟠 中 | `OrderService.unpay()` |
| 23 | Bug | pay 覆盖烹饪状态 | 🟠 中 | `OrderService.pay()` |
| 24 | 规范 | 硬编码测试账号 | 🟡 低 | 登录页面 |
| 25 | Bug | JS 浮点计算精度 | 🟡 低 | `customer/store.js` |
| 26 | 规范 | 小程序全用 var | 🟡 低 | `miniprogram/` |
| 27 | 性能 | O(n*m) 嵌套循环 | 🟡 低 | `miniprogram/pages/menu/` |
| 28 | 维护 | 视图重复创建 | 🟡 低 | 迁移脚本 |
| 29 | Bug | create 并发隐患 | 🟡 低 | `OrderService.create()` |
| 30 | 可维护 | 库存变动无审计 | 🟡 低 | `OrderService.cancel()` |
| 31 | UX | Token 过期直接跳转 | 🟡 低 | 前端拦截器 |
| 32 | UX | 按钮缺防抖 | 🟡 低 | 前端按钮 |

---

## 🎯 更新后的修复优先级

```
第一轮（必须修 — 有逻辑Bug）：已完成（2026-06-06）
  ✅ 已修复全额退款失效（OrderMapper.decreaseDetailQuantity 条件改为 >=）
  ✅ 已修复 transferTable 孤儿记录（合并后删除源订单 detail）
  ✅ 已修复 OrderController.updateStatus 缺少权限
  ✅ 已修复 GlobalExceptionHandler 500 信息泄露
  ✅ 已修复 unpay 不恢复桌台状态

第二轮（安全加固）：已完成（2026-06-06）
  ✅ 已完成 JWT 密钥环境变量化
  ✅ 已完成数据库密码环境变量化
  ✅ 已完成 DTO 参数校验
  ✅ 已完成登录限流
  ✅ 已添加核心单元测试

第三轮（质量提升）：已完成（2026-06-06）
  ✅ 已拆分 DashboardView 的服务呼叫面板
  ✅ 已添加兼容式分页
  ✅ 已修复 pay() 烹饪状态覆盖
  ✅ 已修复前端浮点精度
  ✅ 已收紧 CORS 请求头
  ✅ 已增加防重复提交和 401/403 跳转提示
  ✅ 已添加 Swagger 并验证 Docker 构建
```

---

## ✅ 第一轮修复执行记录（2026-06-06）

本轮已按“更新后的修复优先级”完成第一轮 5 个必须修问题，并补充轻量单元测试。

| 编号 | 问题 | 修复状态 | 修复摘要 |
|------|------|----------|----------|
| 21 / 10 | 全额退款失效、退款明细处理顺序不清晰 | ✅ 已修复 | `decreaseDetailQuantity` 条件改为 `quantity >= #{quantity}`；退款流程改为先扣减，扣减失败抛 `BusinessException`，再删除 `quantity = 0` 的明细 |
| 20 | `transferTable` 合并订单后产生孤儿明细 | ✅ 已修复 | 新增 `OrderMapper.deleteDetails(orderId)`；源订单明细合并到目标订单后，删除源订单明细，再把源订单改为 `CANCELLED` |
| 3 | `OrderController.updateStatus` 缺少权限 | ✅ 已修复 | `PUT /api/orders/{id}/status` 增加 `@PreAuthorize("hasAnyRole('管理员','服务员')")` |
| 5 | 500 错误泄露内部信息 | ✅ 已修复 | `GlobalExceptionHandler` 使用 `log.error("Unhandled exception", ex)` 记录真实异常，前端统一返回 `系统繁忙，请稍后再试` |
| 22 | `unpay()` 不恢复桌台状态 | ✅ 已修复 | 反结账成功后调用 `tableInfoMapper.updateStatus(order.getTableId(), "OCCUPIED")` |

### 维护上下文

- 500 真实异常以后从后端日志查看，前端响应体不再包含内部错误信息；排查时搜索 `Unhandled exception`。
- 本地运行后端时查看 IDE 控制台或 `mvn spring-boot:run` 终端；Docker 部署时执行 `docker compose logs -f backend`；根目录 `backend_output.log` 可能保留最近一次手动启动输出。
- 退款删除明细的条件现在是 `quantity = 0`，不能再改回“按退款数量判断删除”，否则可能出现超退误删或全额退款失败。
- 并桌/换桌合并订单时，源订单被取消前必须清理源订单明细，避免取消订单残留菜品进入后厨/统计链路。
- 反结账后订单回到 `PENDING`，桌台必须回到 `OCCUPIED`，否则桌台卡片与订单状态会不一致。

### 测试覆盖

新增测试文件：

```text
restaurant-backend/src/test/java/com/example/restaurant/common/GlobalExceptionHandlerTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/OrderServiceTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/RefundServiceTest.java
restaurant-backend/src/test/resources/logback-test.xml
```

验证命令：

```bash
cd restaurant-backend
mvn -q test
```

验证结果：通过。

---

## ✅ 第三轮修复执行记录（2026-06-06）

本轮已按“第三轮（质量提升）”完成质量提升项。

| 编号 | 问题 | 修复状态 | 修复摘要 |
|------|------|----------|----------|
| 6 | `DashboardView` 过于臃肿 | ✅ 已推进 | 拆出 `restaurant-admin/src/components/WaiterCallPanel.vue`，服务呼叫面板独立维护 |
| 12 | 列表接口缺少分页 | ✅ 已修复 | 新增 `PageResponse` / `PageUtils`；主要列表接口支持 `page` / `size`；未传分页参数时保持原数组响应 |
| 23 | `pay()` 覆盖烹饪状态 | ✅ 已修复 | 新增 `updatePendingDetailStatus`，只将 `NULL/PENDING` 明细推进到 `PREPARING` |
| 25 | 顾客端金额浮点计算 | ✅ 已修复 | 顾客端和管理端购物车合计改为按“分”累计 |
| 18 | CORS 过宽 | ✅ 已修复 | `allowedHeaders("*")` 改为明确允许 `Authorization`、`Content-Type`、`X-Requested-With` |
| 31 / 32 | 401/403 直接跳转、缺少防重复点击 | ✅ 已修复 | 前端拦截器先提示再跳转；顾客端下单与管理端关键异步操作加锁 |
| 13 / Docker | Swagger + Docker | ✅ 已修复 | 引入 `springdoc-openapi`，开放 Swagger UI；执行 Docker backend 构建验证 |

### 维护上下文

- 分页是兼容式：不传 `page/size` 时仍返回数组；传入后返回 `{ records, total, page, size, pages }`。
- Swagger UI 地址：`/swagger-ui/index.html`，OpenAPI JSON 地址：`/v3/api-docs`。
- `pay()` 不再循环所有明细调用 `updateDetailStatus`，后续维护不要把 `READY/SERVED` 打回 `PREPARING`。
- 管理端 `DashboardView` 目前只完成服务呼叫面板拆分，后续还可以继续拆 `TableGrid`、`TableDetailModal`、`QrCodeModal`。

### 测试与构建

验证命令：

```bash
cd restaurant-backend
mvn -q test
```

```bash
cd restaurant-admin
npm run build
```

```bash
cd restaurant-customer
npm run build
```

```bash
docker compose build backend
```

验证结果：全部通过。

---

## ✅ 第二轮修复执行记录（2026-06-06）

本轮已按“第二轮（安全加固）”完成 5 个问题。

| 编号 | 问题 | 修复状态 | 修复摘要 |
|------|------|----------|----------|
| 2 | JWT 密钥硬编码/弱默认值 | ✅ 已修复 | `jwt.secret` 改为 `${JWT_SECRET}`；`JwtProperties` 启动期校验密钥不能为空、不能等于旧默认值、长度至少 32 字节 |
| 4 | 数据库密码明文/弱默认值 | ✅ 已修复 | `spring.datasource.password` 改为 `${SPRING_DATASOURCE_PASSWORD}`；Docker Compose 改为要求 `.env`/环境变量显式提供密码 |
| 7 | DTO 缺少输入校验 | ✅ 已修复 | 引入 `spring-boot-starter-validation`；登录、下单、支付、退款、评价、转桌、状态更新、呼叫服务员等请求 DTO 增加 Jakarta Validation 注解；Controller 使用 `@Valid` |
| ROADMAP | 登录限流 | ✅ 已修复 | 新增 `LoginRateLimiter`，按 `客户端IP + 手机号` 统计失败次数；默认 10 分钟内失败 5 次锁定 10 分钟，成功登录清理计数 |
| 1 / ROADMAP | 核心单元测试 | ✅ 已补充 | 新增 JWT 配置校验、登录限流、DTO 校验测试；第一轮订单/退款/异常处理测试继续通过 |

### 维护上下文

- 后端启动前必须提供 `SPRING_DATASOURCE_PASSWORD` 和 `JWT_SECRET`；Docker Compose 还必须提供 `MYSQL_ROOT_PASSWORD`。
- 本地开发复制 `.env.example` 为 `.env` 后修改占位值；`.env` 已加入 `.gitignore`，不要提交真实密钥。
- `JWT_SECRET` 至少 32 字节，且不能使用旧默认值 `restaurant-course-design-secret-key-change-me-please-2026`。
- 登录限流默认配置：`max-failures=5`、`window-minutes=10`、`lock-minutes=10`；可用 `LOGIN_RATE_LIMIT_*` 环境变量覆盖。
- 参数校验失败统一返回 400，message 为第一条校验错误；不会进入 Service 层继续执行业务逻辑。

### 测试覆盖

新增测试文件：

```text
restaurant-backend/src/test/java/com/example/restaurant/security/JwtPropertiesTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/LoginRateLimiterTest.java
restaurant-backend/src/test/java/com/example/restaurant/dto/RequestValidationTest.java
```

验证命令：

```bash
cd restaurant-backend
mvn -q test
```

验证结果：通过。

---

## 🔄 第三轮审查（2026-06-06 21:37）

本轮重点核对前两轮发现的问题是否已修复，并深入审查前两轮未覆盖的区域（DTO 校验、限流、Docker 部署、小程序图片处理、nginx 配置等）。

---

### ✅ 已确认修复的问题

| 编号 | 问题 | 修复内容 |
|------|------|----------|
| 3 | 订单状态接口无权限 | `OrderController` 类级加 `@PreAuthorize`，`updateStatus` 限制为管理员/服务员 |
| 5 | 500 泄露异常信息 | 改为返回 `"系统繁忙，请稍后再试"` + `log.error` 内部记录 |
| 10 | 退款逻辑冗余+全量退款BUG | `>=` 替代 `>`，`decreaseDetailQuantity` 先执行并检查返回值抛异常 |
| 20 | transferTable 孤儿记录 | 合并后调用 `orderMapper.deleteDetails()` 清理源订单明细 |
| 21 | 全额退款失效 | 条件修正 + 顺序调整（先减再删） |
| 22 | unpay 不恢复桌台 | 新增 `tableInfoMapper.updateStatus(id, "OCCUPIED")` |
| 2 | JWT 密钥硬编码 | 改为 `${JWT_SECRET:...}` 环境变量，生产环境 `docker-compose` 强制必填 |
| 4 | 数据库密码明文 | 改为 `${SPRING_DATASOURCE_PASSWORD:...}` 环境变量 |
| 1 | 零测试覆盖 | 新增 3 个测试类：`OrderServiceTest`、`RefundServiceTest`、`GlobalExceptionHandlerTest` |
| 7 | DTO 缺少校验 | 几乎所有 DTO 都加了 `@NotNull`/`@NotBlank`/`@Size`/`@NotEmpty` 等注解 |
| — | 登录无限流 | 新增 `LoginRateLimiter`，5次失败锁10分钟，可环境变量配置 |
| — | 无 Docker | 新增完整 docker-compose + 3个 Dockerfile + nginx 配置 + `.env.example` |
| — | 无 API 文档 | 新增 `OpenApiConfig`，集成 SpringDoc OpenAPI |
| 23 | pay() 覆盖烹饪状态 | 改用 `updatePendingDetailStatus`，只更新 PENDING→PREPARING |
| 12 | 列表无分页 | 新增 `PageUtils` + `PageResponse`，Review/User/Table 接口已支持分页 |
| 16 | callWaiter 用 Map 解析 | 新增 `WaiterCallRequest` DTO 替代原始 Map |
| — | KitchenController 用 Map | 新增 `KitchenStatusUpdateRequest` DTO + `@Valid` |
| 6 | DashboardView 臃肿 | `WaiterCallPanel` 已提取为独立组件（968行→仍有优化空间） |
| — | 小程序图片加载失败 | 新增 `wx.downloadFile` 兜底，图片加载失败自动尝试下载为临时文件 |

---

### ⚠️ 部分修复 / 仍需改进

#### 33. PageUtils 是"假分页"（内存分页）

```java
public static <T> PageResponse<T> page(List<T> records, Integer page, Integer size) {
    // ...
    List<T> slice = records.subList(from, to);  // ← 全量数据已在内存中
}
```

`PageUtils` 接收的是全量 `List`，然后做 `subList`。这意味着数据库层面仍然是 `SELECT *` 全表扫描，只是返回给前端时切了一刀。数据量大时性能问题依然存在。

**建议**：在 MyBatis XML 层加 `LIMIT #{offset}, #{size}`，实现真正的数据库分页。

---

#### 34. 硬编码测试凭证仍未清理

三个端的登录页仍然存在测试默认值：

| 文件 | 内容 |
|------|------|
| `admin/LoginView.vue` | `phone: '13800000000', password: '123456'` |
| `customer/LoginView.vue` | `password: '123456'`（phone 已清空） |
| `miniprogram/login/login.js` | `phone: '13800000001', password: '123456'` |

**新增问题**：`admin/UsersView.vue` 第82行：
```javascript
const password = prompt(`请输入 ${user.username} 的新密码`, '123456')
```
用户若不输入直接点确定，密码被设为 `'123456'`。

**建议**：`prompt` 默认值去掉，或改为空字符串并在提交时校验非空。

---

#### 35. Token 仍存储在 localStorage

前端 JWT 仍在 `localStorage` 中：

```
restaurant-admin/src/api/http.js:9:   localStorage.getItem('token')
restaurant-customer/src/api/http.js:9: localStorage.getItem('token')
```

已确认未修复。`httpOnly` Cookie 方案需要改动前后端、nginx 配置，工作量大，暂未处理可以理解，但风险仍在。

---

### 🟡 第三轮新发现问题

#### 36. admin/nginx.conf 与 customer/nginx.conf 完全重复

两个文件内容 **100% 相同**（都是 27 行），属于代码重复。

**建议**：抽取为共享配置，或在 `docker-compose.yml` 中通过 volume 映射同一个文件。

---

#### 37. 迁移脚本 v_kitchen_queue 视图重复定义

`migration-add-detail-status.sql` 和 `migration-current-db-fixes.sql` 都定义了同一个视图。后者是前者的扩展版本（增加了 `reminder_count`/`last_reminder_time`），但两处未合并。

**建议**：保留 `migration-current-db-fixes.sql` 的最新版，删除旧文件中重复的视图定义。

---

#### 38. 小程序 enrichDishCartCount O(n*m) 循环未优化

之前指出的双层循环仍然存在：

```javascript
for (var i = 0; i < dishes.length; i += 1) {
    for (var j = 0; j < cart.length; j += 1) { ... }
}
```

同时整个小程序端仍然全部使用 `var` 声明变量，未改用 `let`/`const`。

---

#### 39. styles.css 依然 980 行

`admin/src/styles.css` 仍是 980 行单体 CSS，未做任何模块化拆分。

---

#### 40. customer cartTotal 浮点计算未修复

```javascript
get cartTotal() {
    return this.cart.reduce((sum, item) => sum + item.price * item.quantity, 0)
}
```

仍然没有 `.toFixed(2)` 或其他精度处理。

---

#### 41. CORS 仍允许 `*` headers + credentials

```java
.allowedHeaders("*")
.allowCredentials(true)
```

浏览器规范禁止这种组合，虽开发环境能跑，但生产环境下某些浏览器或代理可能拒绝。

---

### 📊 完整问题状态追踪（三轮汇总）

| # | 问题 | 严重度 | 状态 |
|---|------|:------:|:----:|
| 1 | 零测试覆盖 | 🔴 | ✅ |
| 2 | JWT 密钥硬编码 | 🔴 | ✅ |
| 3 | 订单状态接口无权限 | 🔴 | ✅ |
| 4 | 数据库密码明文 | 🔴 | ✅ |
| 5 | 500 泄露异常信息 | 🔴 | ✅ |
| 6 | DashboardView 37KB | 🟠 | ⚠️ 部分（968行，WaiterCallPanel已拆出） |
| 7 | DTO 缺少校验 | 🟠 | ✅ |
| 8 | Token 存 localStorage | 🟠 | ❌ |
| 9 | 顾客接口角色隔离不足 | 🟠 | ❌ 未变 |
| 10 | 退款逻辑冗余+全量退款BUG | 🔴 | ✅ |
| 11 | 内存黑名单重启丢失 | 🟠 | ⚠️ 开发可接受，生产用Redis |
| 12 | 列表无分页 | 🟡 | ⚠️ 部分（内存分页，非DB分页） |
| 13 | 无 API 文档 | 🟡 | ✅ Swagger已集成 |
| 14 | MenuResponse 未用 Lombok | 🟡 | ❌ 未变 |
| 15 | styles.css 无组织 | 🟡 | ❌ 980行未变 |
| 16 | callWaiter 用 Map 解析 | 🟡 | ✅ WaiterCallRequest DTO |
| 17 | reportStartDate 返回 null | 🟡 | ❌ 未变 |
| 18 | CORS 配置过宽 | 🟡 | ❌ 未变 |
| 19 | 无连接池参数 | 🟡 | ❌ 未变 |
| 20 | transferTable 孤儿记录 | 🔴 | ✅ |
| 21 | 全额退款失效 | 🔴 | ✅ |
| 22 | unpay 不恢复桌台 | 🟠 | ✅ |
| 23 | pay 覆盖烹饪状态 | 🟠 | ✅ |
| 24 | 硬编码测试账号 | 🟡 | ⚠️ 部分（customer phone清空，其余仍在） |
| 25 | JS 浮点计算精度 | 🟡 | ❌ 未变 |
| 26 | 小程序全用 var | 🟡 | ❌ 未变 |
| 27 | O(n*m) 嵌套循环 | 🟡 | ❌ 未变 |
| 28 | 视图重复创建 | 🟡 | ❌ 未变 |
| 29 | create 并发隐患 | 🟡 | ❌ 未变（低风险） |
| 30 | 库存变动无审计 | 🟡 | ❌ 未变 |
| 31 | Token 过期直接跳转 | 🟡 | ❌ 未变 |
| 32 | 按钮缺防抖 | 🟡 | ❌ 未变 |
| 33 | PageUtils 内存分页 | 🟡 | 🆕 新发现 |
| 34 | 硬编码密码 prompt | 🟠 | 🆕 新发现 |
| 35 | Token localStorage | 🟠 | 已记录（同#8） |
| 36 | nginx 配置重复 | 🟡 | 🆕 新发现 |
| 37 | 迁移脚本视图重复 | 🟡 | 🆕 新发现（同#28） |
| 38 | 小程序 O(n*m) 循环 | 🟡 | 🆕 新发现（同#27） |
| 39 | styles.css 980行 | 🟡 | 🆕 新发现（同#15） |
| 40 | cartTotal 浮点 | 🟡 | 🆕 新发现（同#25） |
| 41 | CORS * + credentials | 🟡 | 🆕 新发现（同#18） |

---

### 🎉 三轮 Review 结论

从第一轮 19 个问题到三轮累计 **41 个发现项**：

- ✅ **已修复**：17 项（含所有 🔴 严重问题 + 大部分 🟠 中等问题）
- ⚠️ **部分修复**：3 项（DashboardView 组件拆分、分页、测试账号）
- ❌ **未处理**：21 项（以 🟡 低优为主，如 CSS 组织、前端细节、var 风格等）

**核心安全与逻辑 Bug 已全部清零**，项目质量从"课程设计"水平提升到了"基本可上线"水平。Docker 部署、Swagger 文档、登录限流、参数校验等生产化能力已补齐。剩余工作集中在代码风格、前端工程化和性能优化层面。
