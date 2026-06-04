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

正式上线必须使用 HTTPS 域名，并在微信公众平台配置 request 合法域名。

## 三、微信登录

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

旧数据库需要执行迁移：

```sql
source restaurant-backend/src/main/resources/sql/migration-add-user-wx-openid.sql;
```

## 四、扫码点单

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

## 五、在微信开发者工具中模拟扫码

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

## 六、管理端桌台二维码

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

## 七、订单找回逻辑

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

## 八、呼叫服务员

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

## 九、图片显示

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
http://localhost:8080/uploads/dishes/xxxxxxxx.png
```

如果图片不显示，请检查：

- 后端是否已重启。
- `GET /uploads/**` 是否已在 Spring Security 中放行。
- 微信开发者工具是否关闭合法域名校验。
- 真机测试时 `API_BASE_URL` 是否改为局域网 IP 或 HTTPS 域名。

## 十、剩余计划

- 接入真实微信支付：`wx.requestPayment()` + 后端支付签名和回调。
- 管理端正式小程序码生成：后端调用微信小程序码接口。
- 真机测试：局域网 IP、HTTPS 域名、合法域名配置。
