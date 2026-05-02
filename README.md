# 餐厅点餐系统

基于数据库课程设计思想实现的前后端分离餐厅点餐系统。

## 技术栈

- 后端：Spring Boot、Spring MVC、MyBatis、Spring Security、JWT、MySQL、Maven
- 前端：Vue 3、Vite、Vue Router、Axios

## 目录

```text
restaurant-backend  后端服务
restaurant-admin    Web管理端
```

## 数据库初始化

1. 创建并初始化表结构：

```sql
source restaurant-backend/src/main/resources/sql/schema.sql;
source restaurant-backend/src/main/resources/sql/data.sql;
```

2. 修改后端数据库连接：

```yaml
restaurant-backend/src/main/resources/application.yml
```

默认数据库为 `restaurant_db`，默认账号密码配置为 `root/root`。

## 默认账号

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 管理员 | 13800000000 | 123456 |
| 服务员 | 13800000001 | 123456 |
| 厨师 | 13800000002 | 123456 |

## 启动

后端：

```bash
cd restaurant-backend
mvn spring-boot:run
```

前端：

```bash
cd restaurant-admin
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 已实现模块

- JWT 登录认证与角色权限控制
- 用户、分类、菜品、桌位管理
- 创建订单、支付订单、取消订单、订单状态流转
- 库存扣减与取消回滚
- 退换菜记录与回库处理
- 评价查询
- 首页看板、月度营收、热销菜品、退换原因统计

## 角色功能说明

| 角色 | 可访问页面 | 功能限制 |
| --- | --- | --- |
| 管理员 | 全部页面 | 全部功能 |
| 服务员 | 首页、菜品统计 | 可开台、点菜、加菜、退菜、关台 |
| 厨师 | 首页、菜品管理、分类管理、菜品统计 | 只读查看桌台订单，可管理菜品/分类/库存 |

## 最近更新 (2026-05-02)

### 角色权限完善

- 路由器增加基于 `meta.roles` 的页面级权限守卫，无权限角色自动重定向至首页
- 侧边导航栏根据当前用户角色动态过滤可见菜单项
- 后端控制器权限注解扩展，厨师角色可访问菜品 CRUD、分类 CRUD 和报表接口
- 桌位查询接口对所有角色开放

### 首页厨师角色视图限制

- 厨师登录后仅可查看桌台订单详情（已点菜品、订单状态），不可操作点菜/加菜/退菜/关台/结账
- 厨师不可点击首页统计卡片（今日订单、今日营业额）
- 厨师不可操作开台

### 订单筛选增强

- 今日订单筛选条件从 `datetime-local` 改为年/月/日/时段（早餐/午餐/下午茶/晚餐/夜宵）下拉选择
- 时段筛选支持跨天夜宵（21:00 — 次日 06:00）

### 数据加载容错

- 首页 `load()` 与 `refreshAfterTableMutation()` 从 `Promise.all` 改为 `Promise.allSettled`，单个接口失败不再阻塞其他数据加载
- 菜品统计页同样改为 `Promise.allSettled`

### 订单金额计算修复

- 修复新订单首次加菜时金额累加逻辑错误：新建订单使用 `updateTotal` 设置初始金额，已有金额的订单使用 `increaseTotal` 累加

### 后端代码规范

- 为 `JwtAuthenticationFilter` 和 `WebConfig` 的方法参数添加 `@NonNull` 注解，消除 Spring 6 null-safety 编译警告
