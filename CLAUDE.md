# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

项目没有 Maven Wrapper，使用本机 Maven：`D:\Program Files\apache-maven-3.9.16`。

```powershell
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' spring-boot:run   # 开发运行
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' clean package     # 构建 jar
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' -Pnative package  # GraalVM native（--static --libc=musl）
```

Playwright 浏览器二进制（首次运行或换机器时需要）：

```powershell
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

手工登录 / 导出 Cookie（Cookie 失效时）：

```powershell
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' exec:java -Dexec.mainClass=com.tzy.api.spider.LoginCapture `
  -Ddouyin.work.dir=D:/work/code/douyin-download
```

接口自检（作品 / 关注 / 收藏，无 JUnit 测试套件）：

```powershell
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' exec:java -Dexec.mainClass=com.tzy.api.spider.BrowserFetchTest
```

仓库里没有 `src/test`，验证依赖上述 main 类与真实抖音接口。

## 配置与本地状态

- `src/main/resources/application.yml`：`app.download.cookie`（Spring 路径使用）、`spring.datasource`（H2 文件
  `./data/douyin`，`ddl-auto: update`）。
- `app.download.base-path` 当前未配置；未配置时 `DownloadService` 的 `Paths.get(basePath, ...)` 会得到相对路径。
- H2 控制台由 `H2ConsoleBoot4Config` 手工注册 `JakartaWebServlet`（Spring Boot 4 下 `spring.h2.console` 不会自动生效），地址
  `/h2-console/*`。
- `application.yml` 中的 Cookie 是真实登录态且已被 git 跟踪；`cookies.txt`、`.browser-profile/`、`login-report.txt` 已
  gitignore。不要把 Cookie 写进日志、提交或回复。
- 本地数据库 `data/douyin.mv.db` 也被 git 跟踪，且体积很大；改动它会产生巨大的 diff。

## 两条并行的代码路径

这个仓库实际存在两套互不调用的实现，改代码前先确认改的是哪一条：

**1. Spring Boot 服务路径（主路径）**

```
Controller → UserService / DataRecordService → DownloadService → Jsoup(HttpUtils)
                                         └→ DataRecord (PENDING) / H2
```

- `/api/users` 管理用户；`/api/users/by-collection` 触发收藏夹发现。
- `/api/records/pending` 分页返回待下载记录，`PUT /api/records/{id}/downloaded` 由外部消费方标记完成。
- `DownloadService.findContentWithHandle` 只抓取作品元数据并保存为 `DataRecord`， **不会真正下载文件**
  。文件下载方法存在但未被这条路径调用；实际的图片/视频下载由读取 records API 的外部程序完成。
- `@Scheduled` 只有收藏夹发现：每 30 分钟执行一次；下载任务已被注释。
- `DataRecordService.saveFromAwemeList`、`UserService.updateLastPostTime` 都是 `@Async`，调用方不要假设落库已完成。
- 重试基本靠递归重入（`findContentWithHandle`、`downloadVideo`、`downloadImage`），没有退避上限，异常时可能无限递归/堆栈增长。

**2. 遗留独立路径（`Tiktok.java` + `DouyinBrowserClient`）**

- `AGENTS.md` 声明 `Tiktok.java` 已废弃，但它仍是唯一能稳定访问抖音签名接口的实现，且未接入 Spring。
- `Tiktok.main` 直接读写硬编码 Windows 路径（`E:\douyin`、`E:\1.txt`、`F:\douyin`），不要把它当作服务入口。
- `DouyinBrowserClient` 用 Playwright 启动无头 Chromium，打开 `https://www.douyin.com/jingxuan`，在**页面上下文**里 `fetch
  ` 接口，由页面 SDK 自动生成 `a_bogus` / `x-secsdk-web-signature` / `uifid`。纯 Java + Jsoup 无法可靠生成这些参数。
- 作品/关注/收藏都通过这一个浏览器实例串行请求（`pageLock`）；改并发前先看锁和 `browserClient()` 生命周期。

## Cookie 与签名相关约束

- `Tiktok` 的 Cookie 优先级：`-Ddouyin.cookie` > `-Ddouyin.cookieFile` > 项目根 `cookies.txt` >
  `src/main/resources/legacy-cookie.txt`。`DownloadService` 只读 `app.download.cookie`，两条路径互不同步。
- 公开接口（作品列表）注入 Cookie 时丢弃 `s_v_web_id` 和旧 `ttwid`，让页面重新生成；登录接口（关注/收藏）必须保留原 `ttwid`（
  `DouyinBrowserClient(cookie, dropTtwid)`）。
- 旧 `s_v_web_id` 或旧 `ttwid` 被风控时，接口返回 **HTTP 200 + 空 body**，不是报错。
- 关注接口 `status=8`、收藏接口 `status=4` 代表登录态失效。`Tiktok` 会自动调用 `LoginCapture` 打开有头浏览器重新登录（5
  分钟内只尝试一次，窗口最长等 10 分钟），成功后重写 `cookies.txt` 并重建浏览器。
- 作品接口未登录仍返回 `status_code=0`，只是内容变少，所以每 10 分钟用关注接口做一次登录态探活。

## 其他实现细节

- 文件名规则：`create_time-desc`，去掉非法字符，超过 200 字符时退回 `aweme_id`；图片按 `0.jpeg`、`1.jpeg` 顺序存目录。
- 合作视频（`cooperation_info.co_creators`）会把同一份内容写入每个作者目录；图片使用硬链接（`Files.createLink`），可能因跨盘/权限失败。
- HTTP 工具 `HttpUtils` 默认随机 UA、信任所有证书、60s 超时、`ignoreHttpErrors(true)`，所以状态码不可信，要看 body。
- Spring Boot 4.1.0 + Java 21；`spring.threads.virtual.enabled: true` 已开启，`@Async` 走虚拟线程。
