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
- Playwright Java 1.62.0 (无头浏览器，页面内请求 API)
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
    ├── DouyinBrowserClient.java    # 无头浏览器内请求作品列表（页面 SDK 自动签名）
    ├── BrowserFetchTest.java       # 作品列表集成自检
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

### 作品列表请求（a_bogus / x-secsdk）

`/aweme/v1/web/aweme/post/` 要求请求携带 `uifid`、`a_bogus`、`x-secsdk-web-signature` 等参数，其中 `a_bogus` 由页面里的
webmssdk/bdms 生成，纯 Java 没有可靠入口。因此 `Tiktok.findPosts` 改为：

1. Playwright 启动无头 Chromium；
2. 加载一次 `https://www.douyin.com/jingxuan`，等待签名 SDK 初始化；
3. 在页面上下文 `fetch('/aweme/v1/web/aweme/post/?...')`，由页面 SDK 自动补签名；
4. 返回并解析 JSON。

首次运行需要安装浏览器：

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

集成自检：

```bash
mvn exec:java -Dexec.mainClass=com.tzy.api.spider.BrowserFetchTest
```

### 登录 / Cookie 刷新

Cookie 失效时，用有头浏览器手动登录一次即可：

```bash
mvn -f D:/work/code/douyin-download/pom.xml exec:java \
  -Dexec.mainClass=com.tzy.api.spider.LoginCapture \
  -Ddouyin.work.dir=D:/work/code/douyin-download
```

- 打开本机 Edge，登录成功后导出 `cookies.txt`，登录态同时保存在 `.browser-profile/`（已 gitignore）
- 只有关注列表接口返回 `status_code=0` 才会导出，避免把 6 小时临时会话写进 `cookies.txt`
- `Tiktok.COOKIE` 优先级：`-Ddouyin.cookie` > `-Ddouyin.cookieFile` > `cookies.txt` > 代码内置旧 cookie
- 实测 `sid_guard` 会话有效期 60 天，`ttwid` 有效期 1 年
- 注入 cookie 时会丢弃 `s_v_web_id`，由页面重新生成；旧的 `s_v_web_id` 会让作品接口返回 HTTP 200 + 空 body

运行中登录态失效时（关注 `status=8`、收藏 `status=4`），`Tiktok` 会自动调用同一套登录流程：

1. 关闭当前浏览器；
2. 打开有头 Edge，等待用户扫码，接口返回 `status_code=0` 后导出 `cookies.txt`；
3. 重新读取 cookie，重试原来的关注/收藏请求并继续分页。

为避免多线程反复弹窗，5 分钟内只自动尝试一次；登录窗口最长等待 10 分钟。

作品接口未登录时仍返回 `status_code=0`，但会少返回“仅粉丝/朋友可见”的内容。因此每 10
分钟会先用关注接口检查一次登录态，失效时同样触发自动重新登录，然后继续抓取作品。
作品请求连续 3 次失败后，也会触发一次自动重新登录再重试，避免只重试、不更新登录态。

`LoginCapture` 默认以 `user.dir` 作为项目目录；如果不是从项目根目录启动，加
`-Ddouyin.work.dir=D:/work/code/douyin-download`。

### ttwid 刷新

`ttwid` 是匿名设备票据，可随时更换，不存在“风控倒计时”：

```text
POST https://www.douyin.com/ttwid/check/           # 页面加载时自动调用，续签当前 ttwid
POST https://www.douyin.com/ttwid/union/register/  # 返回全新的 ttwid（Domain=douyin.com）
```

`register` 请求体：

```json
{"region":"cn","aid":6383,"needFid":false,"service":"www.douyin.com",
 "migrate_info":{"ticket":"","source":"node"},"cbUrlProtocol":"https","union":true}
```

`https://ttwid.bytedance.com/ttwid/union/register/` 也能返回 ttwid，但 `Domain=bytedance.com`，不会发给 `douyin.com`
，不要在抖音接口里用。

### 关注列表 / 收藏夹

`findAllMyFollow` / `findCollects` 也已改为浏览器页面内请求，但它们是 **登录接口**：

- 需要有效的登录 cookie（主要是 `sessionid`）。
- 关注列表返回 `status=8`、收藏返回 `status=4`，通常是 cookie 过期/被风控，或 `collects_id` 失效。
- 所有页面内请求都会丢弃旧 `ttwid`，由页面重新生成；旧 ttwid 被风控时会导致作品接口返回 200 空 body。丢弃 ttwid 不影响登录态。
- `application.yml` 的 `app.download.cookie` 由 `DownloadService` 使用，`Tiktok` 不会读取它。

## 注意事项

1. 需要在浏览器登录抖音网页版后获取Cookie
2. Cookie会过期，需要定期更新
3. 大量下载可能触发平台限制，建议合理控制下载频率
4. 无头浏览器方案依赖 Playwright 浏览器二进制，部署机需预留约 200MB 空间

## 构建

```bash
mvn clean package
```

生成的jar文件位于 `target/` 目录下。