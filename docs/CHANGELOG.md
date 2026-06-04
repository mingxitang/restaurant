# 近期变更记录

本文档记录最近一轮围绕微信小程序顾客端、扫码点单、服务呼叫、二维码和图片显示的改动，方便后续维护时快速理解上下文。

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
GET /uploads/**
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
- 真实微信登录和真实微信支付尚未接入。
- 真实微信支付尚未接入。
