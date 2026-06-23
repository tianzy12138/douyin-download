# Spring Boot 驱动改造设计文档

## 概述

将抖音下载工具从独立 main 方法运行改造为 Spring Boot 驱动的定时任务服务，支持通过 REST API 动态管理用户列表。

## 目标

- 定时自动执行下载任务（固定间隔调度）
- 用户列表通过数据库管理，支持运行时动态添加
- 基础配置（Cookie、路径、调度时间）通过配置文件管理
- 提供 REST API 进行用户管理和手动触发任务
- 失败时有限重试，不阻塞整体任务执行

## 架构设计

### 包结构

```
src/main/java/com/tzy/api/
├── DouyinDownloadApplication.java    # 主入口
├── config/
│   └── ScheduleConfig.java           # 启用 @Scheduled
├── controller/
│   └── UserController.java           # REST API
├── service/
│   ├── DownloadService.java          # 下载核心逻辑
│   └── UserService.java              # 用户管理
├── repository/
│   └── UserRepository.java           # 用户表操作
├── entity/
│   └── User.java                     # 用户实体
├── common/
│   ├── HttpUtils.java                # 保持不变
│   └── ThreadPoolUtils.java          # 保持不变
└── spider/
    ├── Tiktok.java                   # 保持不变，不参与重构
    └── dto/                          # 保持不变
```

### 重构约束

**Tiktok.java 保持不变**

- 原有 `Tiktok.java` 文件不修改、不删除、不引用
- 需要使用的方法复制到 `DownloadService` 中
- 重构完成后可选择删除或保留作为备份

### 数据模型

**User 实体（用户列表）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | UUID 主键 |
| nickname | String | 用户昵称 |
| secUid | String | sec_user_id，用于 API 调用 |
| shareUrl | String | 用户主页分享链接 |
| enabled | Boolean | 是否启用下载，默认 true |
| createdAt | LocalDateTime | 添加时间 |
| lastPostTime | LocalDateTime | 用户作品最后更新时间，用于增量下载判断 |

**数据库选型**：H2 嵌入式数据库，文件存储模式

### REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users | 查询所有用户 |
| POST | /api/users | 添加用户 |
| PUT | /api/users/{id} | 更新用户（启用/禁用） |
| DELETE | /api/users/{id} | 删除用户 |
| POST | /api/tasks/start | 手动触发下载任务 |
| GET | /api/tasks/status | 查询任务状态 |

### 用户添加方式

1. **手动添加** — 通过 POST /api/users 接口，提供 shareUrl 和 nickname
2. **收藏夹自动发现** — 定时任务自动执行 findCollects，获取收藏列表，将新发现的作者添加到用户表（已存在的用户跳过）

### 定时任务

两个独立的定时任务：

| 任务 | 说明 | 默认调度 |
|------|------|----------|
| 下载任务 | 遍历启用用户，下载其作品 | 每天凌晨 2 点 |
| 收藏夹发现 | 从收藏夹发现新用户并自动添加 | 每天凌晨 3 点 |

- 使用 Spring `@Scheduled` 注解
- 调度时间通过 `application.yml` 配置

### 配置管理

**application.yml**

```yaml
app:
  schedule:
    download-cron: "0 0 2 * * ?"      # 下载任务
    collects-cron: "0 0 3 * * ?"      # 收藏夹发现任务
  download:
    base-path: "E:\\douyin"
    cookie: ""
  retry:
    max-attempts: 3
```

### 重试机制

- 失败后最多重试 `max-attempts` 次
- 超过阈值后跳过当前用户，继续执行下一个
- 不阻塞整体任务执行

## 实现要点

### DownloadService 改造

从 `Tiktok.java` 提取核心方法：

| 原方法 | 新方法 | 说明 |
|--------|--------|------|
| job() | downloadUserContent() | 下载单个用户内容 |
| jobList() | downloadAllEnabledUsers() | 下载所有启用用户 |
| findCollects() | downloadFromCollection() | 从收藏夹下载 |
| findProfile() | getUserProfile() | 获取用户信息 |

### UserService 职责

- 用户 CRUD 操作
- 从数据库查询 `enabled=true` 的用户列表
- 替代原有的 `allLine()` 文件读取逻辑

### 配置注入

- Cookie 通过 `@Value("${app.download.cookie}")` 注入
- 路径通过 `@Value("${app.download.base-path}")` 注入
- 替代原有硬编码常量

## 不在范围内

- 下载记录存储
- Web 管理界面
- 认证授权
- 分布式部署支持

## GraalVM Native Image 兼容性

### 构建配置

- Spring Boot 4.x 原生支持 GraalVM Native Image
- 使用 Spring AOT (Ahead-of-Time) 编译
- pom.xml 添加 `spring-boot-maven-plugin` 的 native profile

### 反射配置

以下组件需要反射元数据：

| 组件 | 说明 |
|------|------|
| DTO 类 (spider/dto/*) | FastJSON 序列化/反序列化 |
| Entity 类 (User) | JPA 实体 |
| 配置类 | Spring Boot 自动配置 |

**解决方案**：
- 使用 `@RegisterReflectionForBinding` 注解标记 DTO 类
- 或通过 `reflect-config.json` 显式声明

### 依赖兼容性

| 依赖 | 兼容性 | 说明 |
|------|--------|------|
| FastJSON | 需配置 | 需要反射元数据 |
| Jsoup | 兼容 | 无特殊要求 |
| H2 | 兼容 | Spring Boot 原生支持 |
| Lombok | 兼容 | 编译时处理 |

### 构建命令

```bash
mvn -Pnative native:compile
```

### 注意事项

- 避免 `Class.forName()` 等动态加载
- 避免使用不支持 Native Image 的库
- 配置文件路径使用外部化配置，支持运行时修改
