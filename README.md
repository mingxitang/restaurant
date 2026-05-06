# 餐厅点餐系统

这是一个基于 Spring Boot + Vue 3 的前后端分离餐厅点餐系统，覆盖顾客扫码点餐、服务员桌台管理、后厨制作看板、菜品库存管理、订单结算、评价统计和运营报表等流程。

项目适合作为数据库课程设计、Java Web 课程设计或前后端分离综合实践项目。

## 一、项目结构

```text
restaurant
├─ restaurant-backend    Spring Boot 后端服务
├─ restaurant-admin      管理端：管理员、服务员、厨师使用
├─ restaurant-customer   顾客端：扫码点餐、查看订单、支付评价
├─ ROADMAP.md            后续完善路线
└─ 项目问题总结与知识点.md
```

## 二、技术栈

后端：

- Spring Boot 3
- Spring MVC
- Spring Security
- JWT
- MyBatis
- MySQL
- Maven

前端：

- Vue 3
- Vite
- Vue Router
- Axios
- 原生 CSS

## 三、核心功能

### 1. 顾客端

- 支持通过 URL 参数带入桌号，例如 `/menu?table=3`。
- 支持浏览菜单、分类筛选、搜索菜品。
- 支持菜品图片展示，未上传图片时显示缺省占位。
- 点菜页底部购物车可展开查看未下单菜品。
- 新菜品点击“下单”后直接生成订单。
- 已下单且没有新增菜品时显示“去支付”。
- 订单页展示已点菜品、订单金额、厨房制作状态。
- 支持顾客催单，催单信息会同步到厨房看板。
- 支付页展示已点菜品详情，模拟支付成功后释放桌台。
- 支持提交评价。

### 2. 管理端

- JWT 登录和角色权限控制。
- 用户管理：新增、修改、重置密码、禁用/启用账号、修改角色。
- 分类管理：新增、修改、删除菜品分类。
- 菜品管理：新增、编辑、删除、批量上架/下架、库存预警、上传菜品图片。
- 桌台管理：开台、点菜、加菜、退菜、关台、结账。
- 支持换桌、并桌和合并订单。
- 今日订单支持搜索、金额区间、日期、时段和状态筛选。
- 首页展示今日订单、已收金额、未收金额、空闲桌位、低库存菜品。
- 报表支持热销菜品、低库存菜品、评价筛选、退菜原因统计。

### 3. 后厨看板

- 按菜品制作状态展示厨房队列。
- 支持状态流转：待制作、制作中、待上菜、已上菜。
- 支持按等待时间高亮提醒。
- 顾客催单后，厨房看板显示催单次数和最近催单时间。
- 已支付且所有菜品已上菜后，订单可自动完成。

### 4. 订单与库存

- 一桌一单：同一桌未结账时，加菜会追加到当前订单。
- 同一道菜重复加菜时，合并数量，不重复插入明细。
- 下单扣减库存。
- 退菜可回滚库存，并同步订单金额。
- 支付成功后释放桌台。
- 管理端结账后订单完成并释放桌台。

## 四、数据库初始化

请先启动 MySQL，并创建数据库：

```sql
CREATE DATABASE restaurant_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurant_db;
```

然后按顺序执行：

```sql
source restaurant-backend/src/main/resources/sql/schema.sql;
source restaurant-backend/src/main/resources/sql/data.sql;
```

如果你是在旧数据库上升级，需要再根据实际情况执行迁移脚本：

```sql
source restaurant-backend/src/main/resources/sql/migration-add-order-detail-remark.sql;
source restaurant-backend/src/main/resources/sql/migration-add-detail-status.sql;
source restaurant-backend/src/main/resources/sql/migration-add-order-reminder.sql;
source restaurant-backend/src/main/resources/sql/migration-current-db-fixes.sql;
```

当前默认数据库配置在：

```text
restaurant-backend/src/main/resources/application.yml
```

默认连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/restaurant_db
    username: root
    password: 1234
```

如果你的 MySQL 密码不是 `1234`，请把 `spring.datasource.password` 改成自己的密码。

## 五、默认账号

| 角色 | 手机号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理员 | 13800000000 | 123456 | 拥有全部管理权限 |
| 服务员 | 13800000001 | 123456 | 开台、点菜、退菜、结账 |
| 厨师 | 13800000002 | 123456 | 菜品管理、分类管理、厨房看板 |
| 顾客 | 13800000003 | 123456 | 顾客端点餐、支付、评价 |

## 六、启动项目

### 1. 启动后端

在后端项目目录运行，也就是包含 `pom.xml` 的目录：

```bash
cd restaurant-backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

### 2. 启动管理端

在管理端项目目录运行，也就是包含 `package.json` 的目录：

```bash
cd restaurant-admin
npm install
npm run dev
```

管理端默认地址：

```text
http://localhost:5173
```

### 3. 启动顾客端

在顾客端项目目录运行，也就是包含 `package.json` 的目录：

```bash
cd restaurant-customer
npm install
npm run dev
```

顾客端默认地址：

```text
http://localhost:5174
```

示例扫码点餐地址：

```text
http://localhost:5174/menu?table=1
```

## 七、打包验证

后端编译：

```bash
cd restaurant-backend
mvn -q -DskipTests package
```

管理端打包：

```bash
cd restaurant-admin
npm run build
```

顾客端打包：

```bash
cd restaurant-customer
npm run build
```

## 八、图片上传说明

菜品管理中上传的图片会保存到后端运行目录下：

```text
restaurant-backend/uploads/dishes/
```

数据库中保存的图片路径类似：

```text
/uploads/dishes/xxxxxxxx.jpg
```

后端通过 `/uploads/**` 对外提供静态资源访问。管理端和顾客端的 `vite.config.js` 已配置 `/uploads` 代理到后端，所以开发环境下上传后可以直接在页面看到图片。

如果图片上传成功但页面不显示，请检查：

- 后端是否已启动。
- 管理端或顾客端是否已重启。
- 图片路径是否以 `/uploads/dishes/` 开头。
- 浏览器开发者工具 Network 中图片请求是否为 404。

## 九、角色权限说明

| 角色 | 可访问模块 | 主要限制 |
| --- | --- | --- |
| 管理员 | 全部页面 | 无 |
| 服务员 | 首页、桌台订单、菜品统计 | 不能进入用户管理等管理员专属页面 |
| 厨师 | 首页、菜品管理、分类管理、厨房看板、报表 | 首页只读桌台订单，不能开台、点菜、退菜、结账 |
| 顾客 | 顾客端页面 | 不能访问管理端接口 |

权限控制分两层：

- 前端：通过路由 `meta.roles` 和导航过滤控制页面入口。
- 后端：通过 `@PreAuthorize` 控制接口权限。

前端权限只是为了改善体验，真正的安全控制以后端权限为准。

## 十、Redis（可选）

Redis 是一个**可选的增强组件**，默认不启用，Windows 上无需安装。

### 默认行为

- Windows 开发环境：使用内存级 JWT 黑名单和缓存，开箱即用。
- 无需安装 Redis，项目正常启动和运行。

### 启用 Redis（WSL / Linux）

如果你在 WSL 或 Linux 环境下运行，可以启用 Redis 以获得持久化的 JWT 黑名单和分布式缓存能力。

**1. 确认 Redis 已启动：**

```bash
redis-cli ping  # 应返回 PONG
```

**2. 修改 `application.yml` 三处：**

```yaml
# ① 删除 Redis 自动配置的排除项
# ② app.redis.enabled 改为 true
# ③ 取消 spring.data.redis 注释
```

详细步骤见：[docs/REDIS.md](docs/REDIS.md)

### 启用后新增功能

- **JWT 登出接口** `POST /api/auth/logout`：登录用户调用后 token 立即失效
- **Redis 缓存**：可用 `@Cacheable` 缓存菜品、分类等查询

## 十一、常见问题

### 1. 后端启动失败，提示数据库连接失败

请检查：

- MySQL 是否启动。
- 是否已经创建 `restaurant_db`。
- `application.yml` 中的用户名和密码是否正确。

### 2. 页面请求接口失败

请确认：

- 后端是否运行在 `8080` 端口。
- 前端是否运行在正确目录。
- 浏览器控制台是否有 401、403、404、500 错误。
- 登录是否过期，必要时重新登录。

### 3. 催单时报 `Unknown column 'reminder_count'`

说明旧数据库缺少催单字段。请执行：

```sql
source restaurant-backend/src/main/resources/sql/migration-add-order-reminder.sql;
source restaurant-backend/src/main/resources/sql/migration-current-db-fixes.sql;
```

执行后重启后端。

### 4. 厨房看板没有显示新订单

请检查：

- 订单是否已经成功下单。
- `order_detail.status` 字段是否存在。
- `v_kitchen_queue` 视图是否已创建。
- 旧数据库是否执行过迁移脚本。

### 5. 顾客端图片不显示

请确认前端已重启，因为 `/uploads` 代理配置在 `vite.config.js` 中，修改后需要重启 Vite 开发服务器。

## 十二、后续优化方向

后续计划记录在：

```text
ROADMAP.md
```

已完成前四阶段：稳定性与文档统一、订单业务闭环、顾客端体验增强、管理端运营能力。

后续可以继续完善：

- JWT 密钥改为环境变量。
- 登录失败次数限制。
- 核心业务单元测试。
- 接口测试清单。
- Docker Compose 一键启动。
- 更细粒度的角色权限说明。
