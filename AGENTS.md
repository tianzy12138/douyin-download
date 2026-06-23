# 抖音下载工具 - 开发指南

## 项目概述

基于 Spring Boot 的抖音视频/图片批量下载工具，支持定时任务自动下载和 REST API 管理。

## 构建与运行

```bash
mvn spring-boot:run          # 开发运行
mvn clean package            # 构建 jar
mvn -Pnative native:compile  # 构建 GraalVM Native Image
```

## 配置

编辑 `src/main/resources/application.yml`：

- `app.download.cookie`: 抖音网页端登录 Cookie
- `app.download.base-path`: 下载文件保存路径
- `app.schedule.download-cron`: 下载任务调度时间
- `app.schedule.collects-cron`: 收藏夹发现调度时间

## REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users | 查询所有用户 |
| POST | /api/users | 添加用户 |
| PUT | /api/users/{id} | 更新用户 |
| DELETE | /api/users/{id} | 删除用户 |
| POST | /api/tasks/start | 手动触发下载 |
| GET | /api/tasks/status | 查询任务状态 |

## 技术栈

- Java 21 + Spring Boot 4.1.0
- H2 数据库 + Spring Data JPA
- Jsoup 1.19.1（HTTP 请求）
- FastJSON 2.0.56（JSON 解析）

## 代码结构

```
src/main/java/com/tzy/api/
├── DouyinDownloadApplication.java
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── scheduler/
├── common/
└── spider/dto/
```

## 注意事项

- Cookie 必须从已登录的浏览器获取
- H2 控制台：http://localhost:8080/h2-console
- Tiktok.java 已废弃，逻辑已迁移至 DownloadService
