# 抖音下载工具

一个基于 Spring Boot 的抖音视频和图片批量下载工具。

## 功能特性

- 批量下载指定用户发布的所有视频和图片
- 支持从收藏夹批量下载内容
- 支持关注列表更新检测与下载
- 多线程并发下载
- 自动清理重复文件
- 文件管理（移动、删除空目录等）

## 技术栈

- Java 21
- Spring Boot 4.1.0
- Jsoup 1.19.1 (HTTP请求)
- FastJSON 2.0.56 (JSON解析)
- Lombok
- Guava 33.4.0
- Apache Commons (Lang3, Collections4)

## 项目结构

```
src/main/java/com/tzy/api/
├── DouyinDownloadApplication.java  # 启动类
├── common/
│   ├── HttpUtils.java              # HTTP请求工具类
│   └── ThreadPoolUtils.java        # 线程池工具类
└── spider/
    ├── Tiktok.java                 # 核心下载逻辑
    └── dto/                        # 数据传输对象
        ├── AwemeList.java          # 作品信息
        ├── Author.java             # 作者信息
        ├── Video.java              # 视频信息
        ├── Images.java             # 图片信息
        └── ...                     # 其他DTO类
```

## 使用方法

### 配置

在 `Tiktok.java` 中修改以下配置：

- `COOKIE`: 抖音网页端的登录Cookie
- `BASE_PATH`: 下载文件保存路径
- `RECORD_FILE`: 用户记录文件路径

### 运行

```bash
mvn spring-boot:run
```

或在 IDE 中运行 `DouyinDownloadApplication.java`

### 主要方法

| 方法                                    | 说明             |
|---------------------------------------|----------------|
| `job(url, maxCursor, update)`         | 下载指定用户的作品      |
| `jobList(update)`                     | 批量下载记录文件中的所有用户 |
| `findCollects(maxCursor, collectsId)` | 从收藏夹下载         |
| `findAllMyFollow(maxCursor)`          | 获取关注列表         |

## 注意事项

1. 需要在浏览器登录抖音网页版后获取Cookie
2. Cookie会过期，需要定期更新
3. 大量下载可能触发平台限制，建议合理控制下载频率

## 构建

```bash
mvn clean package
```

生成的jar文件位于 `target/` 目录下。