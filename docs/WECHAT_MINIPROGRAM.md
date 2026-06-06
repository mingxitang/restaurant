# 微信小程序顾客端说明

本文档记录 `restaurant-miniprogram` 的开发、扫码点单、联调和常见问题。

## 一、目录说明

```text
restaurant-miniprogram
├─ miniprogram
│  ├─ api/index.js             小程序业务接口
│  ├─ config/index.js          后端地址和缓存 key
│  ├─ utils/request.js         wx.request 封装
│  └─ pages
│     ├─ login                 登录页
│     ├─ table                 选桌/扫码入口页
│     ├─ menu                  点餐页
│     ├─ order                 订单页
│     └─ pay                   支付/评价页
├─ project.config.json
└─ project.private.config.json
```

## 二、后端地址配置

开发阶段配置文件：

```text
restaurant-miniprogram/miniprogram/config/index.js
```

默认配置：

```js
var API_BASE_URL = 'http://localhost:8080'
```

如果用微信开发者工具模拟器测试，通常可以保持 `localhost`。

如果用真机预览，需要改成电脑局域网 IP，例如：

```js
var API_BASE_URL = 'http://192.168.1.20:8080'
```

注意：

- `localhost` 在真机上表示手机自身，不是开发电脑。
- 局域网 IP 只适合开发阶段调试。
- 正式上线必须使用 HTTPS 域名，并在微信公众平台配置 request、uploadFile、downloadFile 合法域名。

## 三、局域网真机调试

局域网 IP 调试的判断顺序：

1. 电脑浏览器访问 `http://电脑IP:8080/api/tables`。
2. 手机浏览器访问 `http://电脑IP:8080/api/tables`。
3. 微信真机调试访问小程序。

只有第 2 步成功后，才继续排查小程序代码。

如果后端运行在 WSL 中，还需要确认：

```powershell
netsh interface portproxy show all
```

应能看到类似：

```text
0.0.0.0  8080  172.xx.xx.xx  8080
```

如果没有转发规则，需要使用管理员 PowerShell 配置：

```powershell
netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=8080 connectaddress=WSL_IP connectport=8080
New-NetFirewallRule -DisplayName "Restaurant Backend 8080" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Any
```

常见现象：

- `request:fail -118:net::ERR_CONNECTION_TIMED_OUT`：手机无法访问电脑后端，优先检查防火墙、portproxy、手机和电脑是否在同一可互访网络。
- 登录页提示 `request fail` 且 `API_BASE_URL` 仍为 `localhost`：小程序无法稳定访问电脑后端，请改为电脑局域网 IP 或 HTTPS 合法域名。
- 电脑可访问但手机不可访问：通常是 Windows 防火墙、公用网络策略或热点隔离。
- 手机浏览器可访问但小程序不可访问：检查微信开发者工具是否关闭合法域名校验，或改用 HTTPS 合法域名。

## 四、HTTPS 合法域名

稳定真机联调和正式上线建议使用：

```text
https://api.example.com
```

基本链路：

```text
小程序 -> https://api.example.com -> Nginx/网关 -> Spring Boot 8080
```

需要准备：

- 可访问的域名。
- 有效 HTTPS 证书。
- 后端或 Nginx 将 `/api/**` 和 `/uploads/**` 都暴露到 HTTPS。
- 微信公众平台配置 request、uploadFile、downloadFile 合法域名。

小程序配置改为：

```js
var API_BASE_URL = 'https://api.example.com'
```

## 五、微信登录

小程序登录页支持两种登录方式：

- 手机号 + 密码登录。
- 微信一键登录。

微信一键登录流程：

1. 小程序调用 `wx.login()` 获取临时 `code`。
2. 小程序请求后端：

```text
POST /api/auth/wx-login
```

3. 后端调用微信接口 `jscode2session` 换取 `openid`。
4. 后端按 `wx_openid` 查找用户。
5. 如果用户不存在，自动创建一个“顾客”角色用户。
6. 后端返回项目原有 JWT token，小程序后续接口仍使用 `Authorization: Bearer ...`。

后端需要配置：

```bash
WECHAT_APP_ID=你的微信小程序AppID
WECHAT_APP_SECRET=你的微信小程序AppSecret
```

WSL/Linux 示例：

```bash
export WECHAT_APP_ID=你的微信小程序AppID
export WECHAT_APP_SECRET=你的微信小程序AppSecret
```

Windows PowerShell 示例：

```powershell
$env:WECHAT_APP_ID="你的微信小程序AppID"
$env:WECHAT_APP_SECRET="你的微信小程序AppSecret"
```

配置后需要重启后端。

如果微信一键登录出现 `request fail`，先排查网络地址：

- 开发者工具本地调试：确认已关闭合法域名校验，且后端正在运行。
- 真机调试：`API_BASE_URL` 不能使用 `localhost`，需要改为电脑局域网 IP。
- 体验版/正式版：必须使用 HTTPS 合法域名，并在微信公众平台配置 request 合法域名。

如果网络请求能到后端，但返回“后端未配置微信小程序 appId/secret”，再检查 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET`。

旧数据库需要执行迁移：

```sql
source restaurant-backend/src/main/resources/sql/migration-add-user-wx-openid.sql;
```

## 六、扫码点单

桌台入口页面：

```text
pages/table/table
```

支持的参数：

```text
pages/table/table?tableId=1
pages/table/table?tableNumber=A02
pages/table/table?table=A02
pages/table/table?scene=tableId%3D1
pages/table/table?scene=tableNumber%3DA02
pages/table/table?scene=A02
```

说明：

- `tableId` 最稳定，推荐正式二维码使用。
- `tableNumber` 适合人工测试和演示。
- `scene` 用于适配微信小程序码。

## 七、在微信开发者工具中模拟扫码

1. 打开微信开发者工具。
2. 点击顶部“普通编译”旁边的下拉菜单。
3. 选择“添加编译模式”。
4. 启动页面填写：

```text
pages/table/table
```

5. 启动参数填写：

```text
tableId=1
```

或：

```text
tableNumber=A02
```

也可以模拟小程序码 `scene`：

```text
scene=tableId%3D1
```

## 八、管理端桌台二维码

管理端首页的每个桌台卡片提供“二维码”按钮。

点击后会显示：

- 桌台扫码点单二维码
- 对应页面路径
- 下载二维码按钮

新增桌台后，只要该桌台已经保存并出现在桌台列表中，也会自动拥有二维码按钮。

二维码内容目前是页面路径，例如：

```text
pages/table/table?tableId=1
```

适合开发者工具模拟和课程设计演示。

正式上线如需微信可识别的小程序码，需要接入微信官方小程序码接口，由后端调用微信接口生成图片。

生产环境推荐改为小程序码：

```text
page: pages/table/table
scene: tableId=1
```

或使用短参数：

```text
scene: t=1
```

小程序进入后解析 `options.scene`，自动绑定桌台并进入点餐流程。

## 九、订单找回逻辑

小程序进入桌台后，会调用：

```text
GET /api/customer/tables/{tableId}/active-order
```

后端会按桌台找回当前订单。

当前找回范围：

```text
PENDING
PAID
```

因此：

- 待支付订单可继续加菜、支付。
- 已支付但未完成订单可继续查看制作状态和催单。
- 已完成或已取消订单不会作为当前订单找回。

## 十、呼叫服务员

小程序点餐页提供“呼叫服务员”按钮。

调用接口：

```text
POST /api/customer/call-waiter
```

后端写入：

```text
waiter_call
```

管理端首页“服务呼叫”面板会显示待处理呼叫，并支持点击“已处理”。

旧数据库需要执行迁移：

```sql
source restaurant-backend/src/main/resources/sql/migration-add-waiter-call.sql;
```

## 十一、点餐页按钮闪烁

如果点击加菜/减菜按钮时页面明显闪烁，通常是小程序列表被频繁整屏重绘。当前点餐页已做两点处理：

- 购物车数量、筛选列表和合计金额合并为一次 `setData`。
- 移除每次加菜后的成功 toast，避免 toast 叠加造成闪烁感。

如果后续继续改菜单页，尽量避免在一次点击中连续调用多次 `setData` 更新整份 `dishes` / `filteredDishes`。

## 十二、图片显示

菜品图片上传后保存到：

```text
restaurant-backend/uploads/dishes/
```

数据库保存路径类似：

```text
/uploads/dishes/xxxxxxxx.png
```

小程序中会将该路径拼接为：

```text
http://电脑IP:8080/uploads/dishes/xxxxxxxx.png
```

如果图片不显示，请检查：

- 后端是否已重启。
- `/uploads/**` 是否已在 Spring Security 中放行。
- 微信开发者工具是否关闭合法域名校验。
- 真机测试时 `API_BASE_URL` 是否改为局域网 IP 或 HTTPS 域名。

当前菜单页已增加图片失败兜底逻辑：

- `<image>` 加载失败时打印失败 URL。
- 尝试使用 `wx.downloadFile` 下载为临时文件再显示。

如果手机浏览器能打开图片，但小程序仍不显示，优先改用 HTTPS 合法域名。

## 十三、剩余计划

- 接入真实微信支付：`wx.requestPayment()` + 后端支付签名和回调。
- 管理端正式小程序码生成：后端调用微信小程序码接口。
- 将本地局域网调试方案整理为一键脚本或稳定部署方案。
