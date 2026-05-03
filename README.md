# 餐厅点餐系统

基于数据库课程设计思想实现的前后端分离餐厅点餐系统。

## 技术栈

- 后端：Spring Boot、Spring MVC、MyBatis、Spring Security、JWT、MySQL、Maven
- 前端：Vue 3、Vite、Vue Router、Axios

## 目录

```text
restaurant-backend   后端服务
restaurant-admin     Web管理端
restaurant-customer  顾客自助点餐端
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

| 角色   | 手机号      | 密码   |
| ------ | ----------- | ------ |
| 管理员 | 13800000000 | 123456 |
| 服务员 | 13800000001 | 123456 |
| 厨师   | 13800000002 | 123456 |
| 顾客   | 13800000003 | 123456 |

## 启动

后端：

```bash
cd restaurant-backend
mvn spring-boot:run
```

管理端：

```bash
cd restaurant-admin
npm install
npm run dev
```

顾客端：

```bash
cd restaurant-customer
npm install
npm run dev
```

访问：

```text
管理端  http://localhost:5173
顾客端  http://localhost:5174
```

## 已实现模块

- JWT 登录认证与角色权限控制
- 用户、分类、菜品、桌位管理
- 创建订单、支付订单、取消订单、订单状态流转
- 库存扣减与取消回滚
- 退换菜记录与回库处理
- 评价查询
- 首页看板、月度营收、热销菜品、退换原因统计
- 顾客自助点餐端（`restaurant-customer`，端口 5174）
- 后厨显示看板（厨房队列、菜品制作状态追踪）

## 最近更新 (2026-05-03)

### 顾客自助点餐端

- 新增 `restaurant-customer` 项目（Vue 3 + Vite），支持移动端扫码点餐
- 顾客登录 → 选择桌台 → 浏览菜单（分类筛选/搜索）→ 购物车下单 → 查看订单 → 支付 → 评价
- 后端新增 `CustomerController`，提供菜单、下单、查单、支付、评价等专用接口
- `vite.config.js` dev server 端口 5174，proxy `/api` 到 8080

### 后厨显示系统 (KDS)

- `order_detail` 表新增 `status` 列（PENDING/PREPARING/READY/SERVED），追踪菜品制作状态
- 新增 `v_kitchen_queue` 视图，按下单时间排序待制作菜品队列
- 新增 `KitchenView.vue` 厨房看板，按菜品聚合展示、按等待时间分色、支持状态流转
- 新增 `KitchenController` / `KitchenService`，提供队列查询和状态更新接口
- 订单支付时自动将所有明细状态设为 PREPARING，进入厨房队列
- 导航栏新增"厨房看板"入口（管理员和厨师可见），`/kitchen` 路由

## 角色功能说明

| 角色   | 可访问页面                                   | 功能限制                                               |
| ------ | -------------------------------------------- | ------------------------------------------------------ |
| 管理员 | 全部页面                                     | 全部功能                                               |
| 服务员 | 首页、菜品统计                               | 可开台、点菜、加菜、退菜、关台                         |
| 厨师   | 首页、菜品管理、分类管理、菜品统计、厨房看板 | 只读查看桌台订单，可管理菜品/分类/库存，可操作厨房队列 |
| 顾客   | 顾客端（菜单、购物车、订单、支付）           | 自助点餐、支付、评价                                   |

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

### 反结账桌台占用修复

- 反结账操作移除对桌台状态 `OCCUPIED` 的强制设置，避免同台其他未结账订单（原桌台正在进行的点单）被错误吞掉
- 桌台状态应由桌台上实际存在的有效订单决定，反结账只负责回退订单支付状态

### 桌台管理交互优化

- 移除每张桌台卡片上的"改名"按钮，缩小卡片尺寸，避免卡片被撑大
- 移除 `floor-head` 区域的 `+ 添加桌台`入口和独立的添加表单
- 首页右上角新增"编辑"切换按钮，点击进入编辑模式后：
  - 每张桌台卡片变为可编辑表单（名称、区域、人数），可保存修改或删除桌台
  - 末尾出现虚线边框的添加卡片，用于新增桌台
- 编辑模式统一了"加桌台"和"改桌台"两个操作入口，界面更简洁
