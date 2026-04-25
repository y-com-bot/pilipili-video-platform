# 🪨 视频平台 (Video Platform)

> TopView 2026 后端一轮考核项目 - 纯 JavaWeb 打造
>
> 目标：战胜 pilipala、踢踏、慢脚等垃圾平台，让赤石的乐趣重新盛行！

## 🛠️ 技术栈 (Tech Stack)
本项目**严格遵守考核要求**，未引入 SpringBoot、SpringMVC 和 MyBatis 等框架，全程采用原生技术栈栈构建，深入理解底层原理。

* **后端语言**: Java (JDK 21)
* **架构模式**: 原生 MVC 架构 / 单体架构
* **底层通信**: Jakarta Servlet API (适配 Tomcat 10+)
* **数据库**: MySQL 8.0.x
* **项目构建**: Maven
* **鉴权机制**: JWT (JSON Web Token)
* **日志系统**: JDK 自带 `java.util.logging`

## ✨ 核心功能模块 (Features)

### 基础必做功能
* [x] **用户注册与登录**：采用 `SHA-256 哈希 + 随机 Salt 加盐` 算法，拒绝明文存储密码。
* [x] **视频管理系统**：支持视频的发布与列表查询。
* [x] **评论互动系统**：支持对视频发布评论与查看。
* [x] **自研数据库连接池**：纯手写基于 `LinkedList` 的连接池，实现连接的复用与动态扩容。
* [x] **基础 ORM 封装**：通过 Java 反射 (Reflection) 与泛型，手写 `BaseDAO`，实现 ResultSet 到 Entity 的自动映射。

### 进阶挑战功能 (加分项)
* [x] **JWT 单设备登录 (顶号机制)**：基于 `ConcurrentHashMap` 缓存用户 Token，实现用户状态内存级管理与并发安全。
* [x] **AOP 切面鉴权**：利用 `Filter` 过滤器实现全局拦截，抽离 Token 校验逻辑，解耦 Controller 业务。
* [x] **RBAC 权限控制**：将角色 (Role) 注入 JWT 载荷，在 Filter 中实现防垂直越权拦截（如 `/admin` 接口仅限管理员访问）。
* [x] **全局异常处理**：通过 GlobalExceptionFilter 捕获所有运行时异常，统一返回格式并按等级记录日志。
* [x] **点赞与热度排序**：支持对视频和评论的点赞/取消点赞，并实现评论按热度 (点赞数) 倒序展示。

## 📂 项目结构 (Directory Structure)

```text
src/main/java/com/yuan/
 ├── controller/    # Servlet 控制层，负责接收 HTTP 请求与响应
 │    ├── filter/   # 全局过滤器 (包含权限拦截、异常处理)
 ├── service/       # 业务逻辑层，处理核心业务与参数校验
 ├── dao/           # 数据访问层，包含 BaseDAO 与具体实体的 CRUD
 ├── entity/        # 数据实体类 (POJO)，与数据库表一一对应
 ├── exception/     # 自定义异常类体系
 └── utils/         # 通用工具类 (加密、JWT、全局缓存、连接池、统一日志)

手写框架

拆分为多个微服务，清晰分层，职责单一，易于维护与扩展
微服务架构，分布式  docker部署
前后端apifox接口文档
前后端链接

数据库的事务管理

把配置放到配置文件，让代码自动读取配置文件

