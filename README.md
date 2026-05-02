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
