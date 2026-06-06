# 近期变更记录

本文档记录项目近期重要改动，方便后续维护时快速理解上下文。

## 2026-06-06

### 一、Code Review 第一轮修复

根据 `项目CodeReview报告.md` 中“更新后的修复优先级”，本轮完成第一轮必须修的问题，范围集中在后端订单、退款、权限和异常处理。

本轮修复的问题：

| 问题 | 原风险 | 本轮处理 |
| --- | --- | --- |
| 全额退款失效 | 明细数量等于退款数量时，`quantity > #{quantity}` 导致 SQL 不执行，退最后一份菜失败 | `decreaseDetailQuantity` 改为 `quantity >= #{quantity}`，退款流程改为先扣减、再删除数量为 0 的明细 |
| `transferTable` 合并订单后产生孤儿明细 | 源订单改为 `CANCELLED` 后，源订单 `order_detail` 仍留在库里，可能污染厨房队列和统计 | 新增 `OrderMapper.deleteDetails(orderId)`，合并到目标订单后删除源订单明细，再取消源订单 |
| `OrderController.updateStatus` 缺少权限 | 任何已登录角色都可能直接改订单状态 | 增加 `@PreAuthorize("hasAnyRole('管理员','服务员')")` |
| 500 错误泄露内部信息 | SQL 错误、异常消息可能直接返回前端 | `GlobalExceptionHandler` 改为日志记录真实异常，前端统一返回 `系统繁忙，请稍后再试` |
| `unpay()` 不恢复桌台状态 | 支付后桌台为 `FREE`，反结账退回 `PENDING` 时桌台仍空闲，可能被新顾客占用 | `unpay()` 成功后同步桌台状态为 `OCCUPIED` |

### 二、关键实现上下文

退款流程当前语义：

```text
插入 refund_record
扣减 order_detail.quantity
如果扣减失败，抛出 BusinessException("退款数量超过订单明细数量")
删除 quantity = 0 的 order_detail
扣减订单 total_amount
按 stockAction 决定是否恢复库存
```

维护时需要注意：

- `deleteRefundedDetail` 现在只清理已经被扣到 `0` 的明细，不再用退款数量判断是否删除。
- `decreaseDetailQuantity` 返回 `0` 表示没有合法明细被扣减，常见原因是超退、订单号/菜品号错误或明细不存在。
- `transferTable` 在目标桌已有订单时会合并明细；合并完成后源订单明细必须删除，避免取消订单残留菜品。
- `unpay()` 只允许 `PAID` 或 `COMPLETED` 订单反结账，反结账后订单回到 `PENDING`，桌台同步回 `OCCUPIED`。
- 500 真实异常不看前端响应体，排查时看后端日志并搜索 `Unhandled exception`。

### 三、本轮涉及文件

后端代码：

```text
restaurant-backend/src/main/java/com/example/restaurant/controller/OrderController.java
restaurant-backend/src/main/java/com/example/restaurant/common/GlobalExceptionHandler.java
restaurant-backend/src/main/java/com/example/restaurant/service/RefundService.java
restaurant-backend/src/main/java/com/example/restaurant/service/OrderService.java
restaurant-backend/src/main/java/com/example/restaurant/mapper/OrderMapper.java
restaurant-backend/src/main/resources/mapper/OrderMapper.xml
```

新增测试：

```text
restaurant-backend/src/test/java/com/example/restaurant/common/GlobalExceptionHandlerTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/OrderServiceTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/RefundServiceTest.java
restaurant-backend/src/test/resources/logback-test.xml
```

文档补充：

```text
README.md
docs/DOCKER_TROUBLESHOOTING.md
docs/CHANGELOG.md
项目CodeReview报告.md
```

### 四、验证记录

本轮执行并通过：

```bash
cd restaurant-backend
mvn -q test
```

当前测试覆盖重点：

- 500 异常返回统一文案，不泄露内部错误。
- 退款时先扣减明细，再删除数量为 0 的明细。
- 退款数量超过可退明细时抛出业务异常，且不扣金额、不恢复库存。
- 转桌并桌时，源订单明细删除发生在源订单取消前。
- 反结账后桌台状态恢复为 `OCCUPIED`。

### 五、Code Review 第二轮修复

根据 `项目CodeReview报告.md` 中“第二轮（安全加固）”优先级，本轮完成配置安全、DTO 校验、登录限流和核心测试补强。

本轮修复的问题：

| 问题 | 原风险 | 本轮处理 |
| --- | --- | --- |
| JWT 密钥环境变量化 | `application.yml` 带旧默认 secret，泄露代码后可伪造 token | `jwt.secret` 改为 `${JWT_SECRET}`，并在 `JwtProperties` 启动期校验：不能为空、不能等于旧默认值、长度至少 32 字节 |
| 数据库密码环境变量化 | 默认 `root/1234` 容易被误带到部署环境 | `spring.datasource.password` 改为 `${SPRING_DATASOURCE_PASSWORD}`；Docker Compose 改为要求 `.env`/环境变量显式提供密码 |
| DTO 参数校验 | 空桌台、空订单明细、负数退款金额、非法评分等请求会进入 Service 后才失败 | 引入 `spring-boot-starter-validation`，给登录、下单、支付、退款、评价、转桌、状态更新、呼叫服务员等请求 DTO 增加校验 |
| 登录限流 | 登录接口可被暴力尝试账号密码 | 新增 `LoginRateLimiter`，按 `客户端IP + 手机号` 统计失败次数，默认 10 分钟内失败 5 次锁定 10 分钟，成功登录清理计数 |
| 核心单元测试 | 关键业务修复缺少自动化保护 | 新增 JWT 配置校验、登录限流、DTO 校验测试；保留并运行第一轮订单/退款/异常处理测试 |

### 六、第二轮关键实现上下文

环境变量要求：

```text
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
```

Docker Compose 额外要求：

```text
MYSQL_ROOT_PASSWORD
```

本地开发可复制：

```bash
copy .env.example .env
```

然后修改 `.env` 中的占位值。`.env` 和 `.env.*` 已加入 `.gitignore`，只有 `.env.example` 允许提交。

登录限流默认配置：

```yaml
app:
  security:
    login-rate-limit:
      max-failures: 5
      window-minutes: 10
      lock-minutes: 10
```

如果要调整阈值，可通过环境变量覆盖：

```text
LOGIN_RATE_LIMIT_MAX_FAILURES
LOGIN_RATE_LIMIT_WINDOW_MINUTES
LOGIN_RATE_LIMIT_LOCK_MINUTES
```

校验失败返回：

```text
HTTP 400
ApiResponse.code = 400
message = 第一条参数错误信息
```

### 七、第二轮涉及文件

配置与依赖：

```text
.gitignore
.env.example
docker-compose.yml
restaurant-backend/pom.xml
restaurant-backend/src/main/resources/application.yml
restaurant-backend/src/main/java/com/example/restaurant/security/JwtProperties.java
```

校验与限流：

```text
restaurant-backend/src/main/java/com/example/restaurant/common/GlobalExceptionHandler.java
restaurant-backend/src/main/java/com/example/restaurant/service/LoginRateLimiter.java
restaurant-backend/src/main/java/com/example/restaurant/controller/AuthController.java
restaurant-backend/src/main/java/com/example/restaurant/controller/OrderController.java
restaurant-backend/src/main/java/com/example/restaurant/controller/CustomerController.java
restaurant-backend/src/main/java/com/example/restaurant/controller/RefundController.java
restaurant-backend/src/main/java/com/example/restaurant/controller/ReviewController.java
restaurant-backend/src/main/java/com/example/restaurant/controller/KitchenController.java
restaurant-backend/src/main/java/com/example/restaurant/dto/
```

新增测试：

```text
restaurant-backend/src/test/java/com/example/restaurant/security/JwtPropertiesTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/LoginRateLimiterTest.java
restaurant-backend/src/test/java/com/example/restaurant/dto/RequestValidationTest.java
```

### 八、第二轮验证记录

本轮执行并通过：

```bash
cd restaurant-backend
mvn -q test
```

当前测试覆盖重点：

- JWT secret 为空、过短、等于旧默认值时启动期校验失败。
- 登录失败达到阈值后限流，成功登录可清理失败计数。
- 登录、下单、退款、评价等请求 DTO 的关键字段校验。
- 第一轮订单/退款/异常处理测试继续通过。

### 九、Code Review 第三轮修复

根据 `项目CodeReview报告.md` 中“第三轮（质量提升）”优先级，本轮完成质量提升项。

本轮修复的问题：

| 问题 | 原风险 | 本轮处理 |
| --- | --- | --- |
| `DashboardView` 过大 | 服务呼叫、桌台、订单、退菜等逻辑全部挤在一个组件里 | 先拆出 `WaiterCallPanel.vue`，保留原事件流，降低首页组件体积和职责 |
| 列表接口缺少分页 | 数据增长后一次性返回全量列表 | 新增 `PageResponse` / `PageUtils`；订单、菜品、用户、桌台、退款、评价、服务呼叫列表支持 `page` / `size`；未传分页参数时仍返回原数组，兼容现有前端 |
| `pay()` 覆盖烹饪状态 | 支付时可能把 `READY/SERVED` 等状态重置成 `PREPARING` | 新增 `updatePendingDetailStatus`，只更新 `status IS NULL OR status = 'PENDING'` 的明细 |
| 前端金额浮点精度 | JS 浮点计算可能显示出 `0.30000000000000004` 类问题 | 顾客端和管理端购物车合计改为按“分”累计后除以 100 |
| CORS 过宽 | `allowedHeaders("*")` + credentials 不利于生产部署 | CORS 请求头收紧为 `Authorization`、`Content-Type`、`X-Requested-With`，保留本地开发来源 |
| 401/403 直接跳转 | token 失效时表单内容可能直接丢失 | 管理端和顾客端拦截器改为先 `confirm` 提示，再清 token 并跳登录页 |
| 防重复点击 | 快速双击可能重复下单、退菜、结账、转桌 | 顾客端下单入口加锁；管理端关键异步操作用 `runOnce` 锁保护 |
| Swagger + Docker | 缺少接口文档入口和 Docker 构建验证 | 引入 `springdoc-openapi`，开放 `/swagger-ui/**` 和 `/v3/api-docs/**`；执行 `docker compose build backend` 验证通过 |

### 十、第三轮关键实现上下文

分页兼容策略：

```text
不传 page/size：data 仍是原来的数组
传 page 或 size：data 为 { records, total, page, size, pages }
```

这样不会打断现有管理端、顾客端和小程序端调用。

Swagger 入口：

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

支付后的厨房状态推进：

```text
只更新 order_detail.status 为 NULL 或 PENDING 的明细到 PREPARING
不再覆盖 PREPARING / READY / SERVED
```

### 十一、第三轮涉及文件

后端：

```text
restaurant-backend/src/main/java/com/example/restaurant/common/PageResponse.java
restaurant-backend/src/main/java/com/example/restaurant/common/PageUtils.java
restaurant-backend/src/main/java/com/example/restaurant/config/OpenApiConfig.java
restaurant-backend/src/main/java/com/example/restaurant/config/SecurityConfig.java
restaurant-backend/src/main/java/com/example/restaurant/config/WebConfig.java
restaurant-backend/src/main/java/com/example/restaurant/controller/
restaurant-backend/src/main/java/com/example/restaurant/service/OrderService.java
restaurant-backend/src/main/java/com/example/restaurant/mapper/OrderMapper.java
restaurant-backend/src/main/resources/mapper/OrderMapper.xml
restaurant-backend/pom.xml
```

前端：

```text
restaurant-admin/src/components/WaiterCallPanel.vue
restaurant-admin/src/views/DashboardView.vue
restaurant-admin/src/api/http.js
restaurant-customer/src/store.js
restaurant-customer/src/api/http.js
restaurant-customer/src/views/MenuView.vue
```

新增测试：

```text
restaurant-backend/src/test/java/com/example/restaurant/common/PageUtilsTest.java
restaurant-backend/src/test/java/com/example/restaurant/service/OrderServiceTest.java
```

### 十二、第三轮验证记录

本轮执行并通过：

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

### 十三、当日收尾与运行态修复

今天收尾阶段还处理了以下运行态问题和配置上下文：

| 事项 | 处理 |
| --- | --- |
| Docker 后端启动失败 | `LoginRateLimiter` 存在测试用构造器和 Spring 构造器，容器启动时 Spring 未选中带配置注入的构造器。已给生产构造器增加 `@Autowired`，并重新执行 `mvn -q test`。 |
| Docker Redis | `docker-compose.yml` 增加 Redis 服务，backend 默认通过 `redis:6379` 使用 Redis 黑名单和缓存；`docs/REDIS.md` 已补充 Docker Compose 场景。 |
| 微信一键登录配置 | backend 从 `.env` / 环境变量读取 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET`，小程序端不保存 AppSecret。 |
| 小程序 request fail 提示 | 曾短暂加入更详细的网络失败提示，后按要求回退为原始 `error.errMsg` / 通用提示。 |
| 小程序按钮闪烁 | 登录、呼叫服务员、下单按钮去掉原生 loading spinner，菜单页加减菜合并 `setData`，减少闪烁。 |

最终运行态验证：

```bash
docker compose up -d backend
```

```text
GET http://localhost:8080/api/tables?page=1&size=2 -> 200
GET http://localhost:8080/v3/api-docs -> 200
```

## 2026-06-05

### 一、局域网真机调试排障

确认小程序真机调试不能使用：

```text
http://localhost:8080
```

原因是 `localhost` 在手机上指向手机自身，不是开发电脑。真机调试应使用电脑局域网 IP，例如：

```js
var API_BASE_URL = 'http://192.168.0.101:8080'
```

本轮排障确认的判断顺序：

1. 电脑浏览器访问 `http://电脑IP:8080/api/tables`。
2. 手机浏览器访问 `http://电脑IP:8080/api/tables`。
3. 微信真机调试访问小程序。

如果第 2 步失败，优先排查网络和防火墙，不改小程序业务代码。

### 二、WSL 后端端口暴露

确认后端运行在 WSL 中时，Spring Boot 日志停在：

```text
Started RestaurantApplication
```

是正常现象，表示后端正在等待请求。

本轮确认过的 WSL 排障点：

- WSL 内部 `*:8080` 由 Java 进程监听。
- Windows `localhost:8080` 能访问后端时，说明 WSL 到 Windows localhost 转发正常。
- 手机访问电脑 IP 超时时，通常是 Windows 防火墙、`portproxy` 或网络隔离问题。
- `netsh interface portproxy show all` 可确认端口转发是否存在。

当前可用的转发形态：

```text
0.0.0.0:8080 -> WSL_IP:8080
```

### 三、Windows 防火墙

确认过的现象：

- 电脑自己访问 `http://电脑IP:8080/api/tables` 成功。
- 手机访问同一地址超时。
- Windows 网络配置为 Public 且入站策略为 `BlockInbound`。
- 未配置 8080 入站放行规则时，手机真机请求会出现：

```text
request:fail -118:net::ERR_CONNECTION_TIMED_OUT
```

解决方式是在管理员 PowerShell 放行 8080：

```powershell
New-NetFirewallRule -DisplayName "Restaurant Backend 8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Any
```

### 四、小程序图片显示

确认菜品图片接口返回路径类似：

```text
/uploads/dishes/31e80993-c767-48ae-8164-d9366dcc1fe8.png
```

小程序端会拼接为：

```text
http://192.168.0.101:8080/uploads/dishes/31e80993-c767-48ae-8164-d9366dcc1fe8.png
```

本轮修复：

- 后端将 `/uploads/**` 改为公开放行，不再只限定 `GET /uploads/**`。
- 小程序菜单页 `<image>` 增加 `binderror` 日志。
- 图片加载失败时，尝试使用 `wx.downloadFile` 下载为临时文件再显示。

如果手机浏览器能打开图片，但小程序真机仍不显示，优先切换 HTTPS 合法域名。

### 五、HTTPS 合法域名与生产小程序码规划

当前结论：

- 局域网 IP 适合本地开发。
- HTTPS 合法域名适合稳定真机联调和正式发布。
- 生产环境桌台入口应接入微信官方小程序码，每个桌台生成一个小程序码。

推荐生产扫码参数：

```text
page: pages/table/table
scene: tableId=1
```

后续待做：

- 准备 HTTPS 后端域名。
- 在微信公众平台配置 request、uploadFile、downloadFile 合法域名。
- 管理端二维码从普通路径二维码升级为官方小程序码。

## 2026-05-25

### 一、微信小程序顾客端

新增项目：

```text
restaurant-miniprogram
```

已完成页面：

- 登录页：手机号 + 密码登录，保存 JWT token。
- 选桌页：加载桌台、手动选桌、支持扫码参数自动进入桌台。
- 点餐页：分类、搜索、菜品列表、购物车、下单、呼叫服务员。
- 订单页：查看订单详情、刷新、催单、跳转支付/评价。
- 支付页：模拟支付、评价提交。

关键能力：

- 支持扫码参数：

```text
tableId
tableNumber
table
scene
```

- 支持按桌台找回当前订单：

```text
GET /api/customer/tables/{tableId}/active-order
```

- 当前订单找回范围：

```text
PENDING
PAID
```

- 点餐页菜品数量控件改为圆形 `+/-` 步进器，支持键盘输入份数。
- 点餐页新增“换桌”按钮，切换前弹出确认框，并清理本地桌台、购物车和当前订单缓存。
- 点餐页新增“呼叫服务员”，会写入后端服务呼叫记录。
- 登录页新增微信一键登录，调用 `wx.login()` 获取 code，并请求后端换取 JWT。

### 二、后端接口与数据库

新增接口：

```text
POST /api/auth/wx-login
GET  /api/customer/tables/{tableId}/active-order
POST /api/customer/call-waiter
GET  /api/waiter-calls?status=PENDING
PUT  /api/waiter-calls/{id}/handle
```

新增后端类：

```text
entity/WaiterCall.java
mapper/WaiterCallMapper.java
service/WaiterCallService.java
controller/WaiterCallController.java
resources/mapper/WaiterCallMapper.xml
```

新增数据库表：

```text
waiter_call
```

用户表新增字段：

```text
user.wx_openid
```

新增迁移脚本：

```text
restaurant-backend/src/main/resources/sql/migration-add-waiter-call.sql
restaurant-backend/src/main/resources/sql/migration-add-user-wx-openid.sql
```

订单找回 SQL 调整：

- 原来只查 `PENDING`
- 现在查 `PENDING` 和 `PAID`

图片访问修复：

- 后端放行：

```text
/uploads/**
```

- 解决上传图片存在但页面显示失败、图片请求 403 的问题。

### 三、管理端

首页新增：

- 服务呼叫面板。
- 待处理呼叫列表。
- “已处理”按钮。
- 每 5 秒刷新一次待处理呼叫。

桌台卡片新增：

- “二维码”按钮。
- 二维码弹窗。
- 下载二维码图片。

移除：

- 桌台卡片上的“复制扫码路径”按钮，避免卡片过大。

菜品管理修复：

- 上传图片后，表格和编辑预览统一通过可访问 URL 展示图片。
- 配合后端 `/uploads/**` 放行，解决图片显示失败。

新增管理端依赖：

```text
qrcode
```

### 四、文档更新

更新：

```text
README.md
ROADMAP.md
docs/WECHAT_MINIPROGRAM.md
docs/CHANGELOG.md
```

新增小程序专项文档：

```text
docs/WECHAT_MINIPROGRAM.md
```

内容覆盖：

- 小程序目录结构。
- 后端地址配置。
- 开发者工具模拟扫码。
- 管理端二维码。
- 订单找回逻辑。
- 呼叫服务员。
- 图片显示排查。
- 剩余计划。

### 五、验证记录

最近执行并通过：

```bash
cd restaurant-backend
mvn -q -DskipTests package
```

```bash
cd restaurant-admin
npm run build
```

数据库迁移已执行过：

```bash
mysql -uroot -p1234 restaurant_db < restaurant-backend/src/main/resources/sql/migration-add-waiter-call.sql
```

### 六、仍需注意

- 小程序真机测试时，`API_BASE_URL` 不能继续使用 `localhost`，需要改为局域网 IP 或 HTTPS 域名。
- 正式上线小程序必须配置微信合法域名。
- 管理端当前二维码内容是小程序页面路径，适合开发者工具和课程设计演示；正式可扫的小程序码还需要接入微信官方小程序码接口。
- 真实微信支付尚未接入。
