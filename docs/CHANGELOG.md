# 近期变更记录

本文档记录最近一轮围绕微信小程序顾客端、扫码点单、服务呼叫、二维码和图片显示的改动，方便后续维护时快速理解上下文。

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
