# Docker 部署与登录问题排查记录

本文记录本项目最近一次 Docker 化部署过程中遇到的问题、根因和处理方式，方便后续维护时快速定位类似现象。

## 当前 Docker 部署结构

项目使用 `docker-compose.yml` 一次启动以下服务：

- `restaurant-mysql`：MySQL 8.0 数据库
- `restaurant-backend`：Spring Boot 后端，端口 `8080`
- `restaurant-admin`：管理端 Vue 构建产物，由 Nginx 提供服务，端口 `5173`
- `restaurant-customer`：顾客端 Vue 构建产物，由 Nginx 提供服务，端口 `5174`

启动命令：

```powershell
docker compose up -d --build
```

查看状态：

```powershell
docker compose ps
```

查看日志：

```powershell
docker compose logs -f backend
docker compose logs -f admin
docker compose logs -f mysql
```

## 1. MySQL 端口绑定失败

### 现象

启动时出现：

```text
ports are not available: exposing port TCP 0.0.0.0:3306
Only one usage of each socket address is normally permitted
```

或：

```text
ports are not available: exposing port TCP 0.0.0.0:3307
An attempt was made to access a socket in a way forbidden by its access permissions
```

### 原因

- `3306` 可能已经被本机 MySQL 占用。
- `3307` 可能被 Windows 保留或 Docker 无权绑定。
- 本项目后端和 MySQL 都在 Docker 内部网络中，后端连接 `mysql:3306`，并不要求把 MySQL 暴露到宿主机。

### 处理

删除 MySQL 的 `ports` 映射，只保留 Docker 内部网络访问。

后端连接地址保持：

```text
jdbc:mysql://mysql:3306/restaurant_db
```

如果后续确实要用 Navicat/DataGrip 从宿主机连接容器内 MySQL，再单独选择一个可用端口映射。

## 2. 登录后长时间无响应

### 现象

管理端打开：

```text
http://localhost:5173/login
```

点击登录后页面看起来长时间无响应，浏览器控制台还出现一些 `chrome-extension://...` 或 `userscript.html...` 报错。

### 实际排查结果

这些浏览器报错来自 Chrome 扩展或油猴脚本，不是项目本身的主要问题。

真正的问题链路是：

```text
POST /api/auth/login 返回 200
登录成功后跳转 /dashboard
首页请求 /api/reports/dashboard 返回 403 权限不足
前端 axios 响应拦截器清掉 token 并跳回 /login
用户看到的效果像是登录没反应
```

### 根因

Docker MySQL 初始化执行 `data.sql` 时没有显式设置客户端字符集，导致中文数据乱码。

登录返回的角色名本应是：

```text
管理员
```

实际变成：

```text
ç®¡ç†å‘˜
```

JWT 里的 `role` 也随之乱码，而后端权限注解要求：

```java
@PreAuthorize("hasAnyRole('管理员','服务员','厨师')")
```

所以登录接口本身成功，但后续受保护接口判断角色失败，返回 `403`。

### 修复

在 `restaurant-backend/src/main/resources/sql/data.sql` 中增加：

```sql
SET NAMES utf8mb4;
```

对于已经初始化过的 Docker 数据库，执行：

```powershell
docker cp restaurant-backend\src\main\resources\sql\fix-docker-charset-data.sql restaurant-mysql:/tmp/fix-docker-charset-data.sql
docker exec restaurant-mysql mysql --default-character-set=utf8mb4 -uroot -p1234 restaurant_db -e "source /tmp/fix-docker-charset-data.sql"
```

修复后验证：

```powershell
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:5173/api/auth/login' -ContentType 'application/json' -Body '{"phone":"13800000000","password":"123456"}'
$login.data
```

应看到：

```text
roleName: 管理员
```

再验证受保护接口：

```powershell
$token = $login.data.token
Invoke-WebRequest -Uri 'http://localhost:5173/api/reports/dashboard' -Headers @{ Authorization = "Bearer $token" }
```

应返回 `200`。

## 3. 登录类问题通用排查流程

遇到“登录后没反应”“登录后又回到登录页”“按钮一直登录中”时，按下面顺序排查。

### 先看浏览器 Network

打开：

```text
F12 -> Network -> 勾选 Preserve log
```

重新登录，重点看：

```text
POST /api/auth/login
GET /api/reports/dashboard
GET /api/orders
GET /api/其他首页初始化接口
```

判断方式：

- `/api/auth/login` 返回 `200`：账号密码登录本身成功。
- `/api/auth/login` 返回 `401/403`：登录接口、账号密码、接口放行或权限配置有问题。
- 登录后首页接口返回 `401/403`：token 没带上、token 无效、角色权限不匹配。
- 登录后首页接口返回 `500`：后端业务、数据库或 SQL 有错误。前端响应只会返回统一提示，真实异常需要看后端日志。
- 请求一直 `pending`：后端卡住、数据库连接失败或 Nginx 代理异常。

### 500 错误查看后端日志

后端全局异常处理会把未捕获异常记录到日志，并向前端返回统一文案：

```text
系统繁忙，请稍后再试
```

所以排查 `500` 时，以后端日志为准：

```powershell
docker compose logs -f backend
```

如果不是 Docker 启动，而是在本机直接运行后端，则查看 IDE 控制台或 `mvn spring-boot:run` 所在终端。项目根目录下的 `backend_output.log` 也可能保留最近一次手动启动后端时的输出。

日志中搜索：

```text
Unhandled exception
```

该行后面的异常堆栈就是真实错误上下文，例如 SQL 错误、空指针、权限配置或数据库连接问题。

### 再看 token

浏览器 Console 执行：

```js
localStorage.getItem('token')
localStorage.getItem('user')
```

如果 token 是 `null`：

- 前端没有保存 token。
- 或保存后被响应拦截器清掉。

如果 token 存在但接口仍然 `401/403`：

- 检查请求头有没有 `Authorization: Bearer xxx`。
- 检查后端 JWT 解析是否成功。
- 检查 JWT 中的角色和 `@PreAuthorize` 要求是否一致。

### 绕过前端直接测接口

登录：

```powershell
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:5173/api/auth/login' -ContentType 'application/json' -Body '{"phone":"13800000000","password":"123456"}'
$token = $login.data.token
$login.data
```

带 token 请求首页：

```powershell
Invoke-WebRequest -Uri 'http://localhost:5173/api/reports/dashboard' -Headers @{ Authorization = "Bearer $token" }
```

如果命令行正常，重点查前端页面逻辑。

如果命令行也失败，重点查后端、数据库、权限或 Nginx 代理。

## 4. 常见根因清单

- token 没保存到 `localStorage`
- axios 请求拦截器没有带 `Authorization`
- 响应拦截器遇到 `401/403` 后清掉 token
- JWT 密钥变更导致旧 token 失效
- JWT 中角色名和后端 `@PreAuthorize` 不匹配
- 数据库中文乱码导致角色名不匹配
- 首页初始化接口返回 `403/500`
- Nginx `/api/` 代理路径配置错误
- 后端连不上 MySQL
- 浏览器扩展或油猴脚本干扰页面

## 5. 建议维护习惯

- Docker 初始化 SQL 中统一显式写 `SET NAMES utf8mb4;`
- 修改权限、角色、JWT 后，必须验证登录后首页接口
- 出现登录异常时，先看 Network 和接口响应，不要先被浏览器扩展报错带偏
- 接口返回 `500` 时，先查 `docker compose logs -f backend` 或本地后端控制台中的 `Unhandled exception`
- Docker 排查优先使用 `docker compose ps` 和 `docker compose logs`
- 修改数据库初始化脚本后，如果要完全重建数据，执行：

```powershell
docker compose down -v
docker compose up -d --build
```

注意：`down -v` 会删除数据库卷，现有数据会丢失。
