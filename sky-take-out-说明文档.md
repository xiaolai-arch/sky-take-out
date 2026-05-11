# 苍穹外卖
### 项目结构
#### 模块划分--整体
1. sky-take-out maven父工程，统一管理依赖版本，聚合其他子模块
2. sky-common 子模块，存放公共类，eg：工具类、常量类、异常类。
3. sky-server 子模块，后端服务，存放配置文件、Controller、Service、Mapper等。
   1. 存放配置文件、配置类、拦截器、controller、service、mapper、启动类等。
4. sky-pojo 子模块，存放实体类。
   1. Entity 实体，通常和数据库中表对应
   2. DTO数据传输对象，通常用于程序各层之间传递数据
   3. VO视图对象，为前端展示数据提供的对象
   4. POJO普通java对象，只有数学和对应的getter和setter

---

## 环境依赖

| 组件 | 版本 | 路径/说明 |
|------|------|-----------|
| JDK | Oracle JDK 21.0.11 | `/Library/Java/JavaVirtualMachines/jdk-21.jdk` |
| Maven | 3.9.15 | `/usr/local/Cellar/maven/3.9.15` |
| MySQL | 8.0.46 | `/usr/local/mysql-8.0.46-macos15-x86_64/bin/mysql` |
| Redis | - | `localhost:6379`（需密码认证） |
| nginx | 1.29.8 | `/usr/local/opt/nginx/bin/nginx`（Homebrew 安装） |
| Spring Boot | 2.7.3 | 项目父工程 |

---

## 数据库环境搭建

### MySQL 连接信息

| 配置项 | 值 |
|--------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库名 | sky_take_out |
| 用户名 | root |
| 密码 | 116013.jm |

配置文件：`sky-server/src/main/resources/application-dev.yml`

通过数据库建表语句创建数据库结构（数据库 `sky_take_out` 已存在）。

---

## 后端启动

### 启动类
`sky-server/src/main/java/com/sky/SkyApplication.java`

### 运行端口
`8080`

### Maven 构建命令
```bash
# 使用 JDK 21 编译（重要：Maven 默认可能指向其他 JDK 版本）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn clean install -DskipTests
```

### 启动命令
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
cd sky-server && mvn spring-boot:run
```

或在 IDEA 中运行 SkyApplication main 方法（需将项目 SDK 设为 JDK 21）。

### 关键配置项

| 配置 | 值 | 说明 |
|------|-----|------|
| 服务端口 | 8080 | `application.yml` - server.port |
| 数据库驱动 | com.mysql.cj.jdbc.Driver | Druid 连接池 |
| 数据库连接池 | Druid 1.2.1 | Alibaba |
| MyBatis 驼峰映射 | true | 自动下划线转驼峰 |
| JWT 秘钥 | itcast | `application.yml` - sky.jwt.admin-secret-key |
| JWT 过期时间 | 7200000ms（2小时） | sky.jwt.admin-ttl |
| JWT 令牌名 | token | 前端请求头中的令牌名称 |

---

## 前端

### 文件位置
`nginx-1.20.2/html/sky/`（Vue.js SPA 编译产物）

### 访问地址
**http://localhost:8088**

### nginx 配置

| 配置项 | 值 |
|--------|-----|
| 监听端口 | 8088 |
| 前端根路径 | `/Users/mac/IdeaProjects/sky-take-out/nginx-1.20.2/html/sky` |
| API 代理 | `/api/` → `localhost:8080/admin/` |
| 用户端代理 | `/user/` → `localhost:8080/user/`（upstream webservers） |
| WebSocket | `/ws/` → 后端，HTTP 升级 + 3600s 超时 |
| 配置文件 | `/usr/local/etc/nginx/nginx.conf` |

### nginx 操作命令
```bash
# 测试配置
/usr/local/opt/nginx/bin/nginx -t

# 启动
/usr/local/opt/nginx/bin/nginx

# 重新加载
/usr/local/opt/nginx/bin/nginx -s reload

# 停止
/usr/local/opt/nginx/bin/nginx -s stop
```

### 请求链路

```
浏览器 http://localhost:8088
  │
  ├─ /index.html、/js/*、/css/*  → nginx 返回前端静态文件
  │
  ├─ /api/xxx     → nginx 反向代理 → localhost:8080/admin/xxx（后端）
  ├─ /user/xxx    → nginx 反向代理 → localhost:8080/user/xxx（后端）
  └─ /ws/         → nginx WebSocket 代理 → 后端
```

---

## 登录信息

- 访问地址：http://localhost:8088
- 默认账号：admin / 123456

---

## 本次环境搭建变更记录

| 变更 | 文件 | 说明 |
|------|------|------|
| 数据库密码 | `application-dev.yml` | `root` → `116013.jm` |
| Lombok 升级 | `pom.xml` | `1.18.20` → `1.18.36`（兼容 JDK 21） |
| Lombok scope | 3 个子模块 pom.xml | 添加 `<scope>provided</scope>` |
| 编译器插件 | `pom.xml` | 新增 maven-compiler-plugin 3.11.0 + Lombok 注解处理器路径 |
| nginx 端口 | `/usr/local/etc/nginx/nginx.conf` | `80` → `8088`（避免 sudo） |
| nginx 前端路径 | `/usr/local/etc/nginx/nginx.conf` | 改为绝对路径指向 `html/sky` |