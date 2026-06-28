# Spring Boot 驱动改造实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将抖音下载工具改造为 Spring Boot 驱动的定时任务服务，支持 REST API 管理用户。

**Architecture:** 轻量重构，在现有代码基础上添加 Spring Boot 分层结构。User 实体 + H2 数据库存储用户列表，DownloadService 复制 Tiktok 核心方法，定时任务自动执行下载和收藏夹发现。

**Tech Stack:** Java 21, Spring Boot 4.1.0, H2, Spring Data JPA, FastJSON 2.0.56, Jsoup 1.19.1

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `pom.xml` | 修改 | 添加 H2、JPA、Validation 依赖，配置 Native Image |
| `src/main/resources/application.yml` | 创建 | 配置文件 |
| `src/main/java/com/tzy/api/entity/User.java` | 创建 | 用户实体 |
| `src/main/java/com/tzy/api/repository/UserRepository.java` | 创建 | 用户数据访问 |
| `src/main/java/com/tzy/api/service/UserService.java` | 创建 | 用户管理服务 |
| `src/main/java/com/tzy/api/service/DownloadService.java` | 创建 | 下载核心逻辑（复制自 Tiktok） |
| `src/main/java/com/tzy/api/controller/UserController.java` | 创建 | REST API |
| `src/main/java/com/tzy/api/config/ScheduleConfig.java` | 创建 | 启用定时任务 |
| `src/main/java/com/tzy/api/scheduler/DownloadScheduler.java` | 创建 | 定时任务调度器 |
| `src/main/java/com/tzy/api/DouyinDownloadApplication.java` | 修改 | 添加 @EnableScheduling |
| `src/main/resources/META-INF/native-image/reflect-config.json` | 创建 | GraalVM 反射配置 |

---

## Task 1: 更新 pom.xml 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 添加 H2 数据库依赖**

在 `<dependencies>` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **Step 2: 添加 GraalVM Native Image 支持**

在 `<build><plugins>` 后添加：

```xml
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.graalvm.buildtools</groupId>
                    <artifactId>native-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

- [ ] **Step 3: 验证 pom.xml 语法**

Run: `mvn validate`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add pom.xml
git commit -m "build: 添加 H2、JPA、Validation 依赖和 Native Image 支持"
```

---

## Task 2: 创建配置文件

**Files:**
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/douyin
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  h2:
    console:
      enabled: true
      path: /h2-console

app:
  schedule:
    download-cron: "0 0 2 * * ?"
    collects-cron: "0 0 3 * * ?"
  download:
    base-path: "E:\\douyin"
    cookie: ""
  retry:
    max-attempts: 3
```

- [ ] **Step 2: 删除默认的 application.properties（如果存在）**

Run: `Remove-Item -Path "src\main\resources\application.properties" -ErrorAction SilentlyContinue`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "config: 创建 application.yml 配置文件"
```

---

## Task 3: 创建 User 实体

**Files:**
- Create: `src/main/java/com/tzy/api/entity/User.java`

- [ ] **Step 1: 创建 User.java**

```java
package com.tzy.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @Column(length = 36)
    private String id;
    
    @Column(length = 100)
    private String nickname;
    
    @Column(length = 200, unique = true)
    private String secUid;
    
    @Column(length = 500)
    private String shareUrl;
    
    private Boolean enabled = true;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime lastPostTime;
    
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/entity/User.java
git commit -m "feat: 创建 User 实体类"
```

---

## Task 4: 创建 UserRepository

**Files:**
- Create: `src/main/java/com/tzy/api/repository/UserRepository.java`

- [ ] **Step 1: 创建 UserRepository.java**

```java
package com.tzy.api.repository;

import com.tzy.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    List<User> findByEnabledTrue();
    
    Optional<User> findBySecUid(String secUid);
    
    boolean existsBySecUid(String secUid);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/repository/UserRepository.java
git commit -m "feat: 创建 UserRepository 接口"
```

---

## Task 5: 创建 UserService

**Files:**
- Create: `src/main/java/com/tzy/api/service/UserService.java`

- [ ] **Step 1: 创建 UserService.java**

```java
package com.tzy.api.service;

import com.tzy.api.entity.User;
import com.tzy.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public List<User> findEnabledUsers() {
        return userRepository.findByEnabledTrue();
    }
    
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    @Transactional
    public User addUser(String shareUrl, String nickname) {
        String secUid = extractSecUid(shareUrl);
        if (userRepository.existsBySecUid(secUid)) {
            throw new IllegalArgumentException("用户已存在: " + secUid);
        }
        User user = new User();
        user.setShareUrl(shareUrl);
        user.setSecUid(secUid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }
    
    @Transactional
    public User addUserIfNotExists(String shareUrl, String nickname) {
        String secUid = extractSecUid(shareUrl);
        Optional<User> existing = userRepository.findBySecUid(secUid);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = new User();
        user.setShareUrl(shareUrl);
        user.setSecUid(secUid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateEnabled(String id, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }
    
    @Transactional
    public void updateLastPostTime(String secUid, LocalDateTime lastPostTime) {
        userRepository.findBySecUid(secUid).ifPresent(user -> {
            user.setLastPostTime(lastPostTime);
            userRepository.save(user);
        });
    }
    
    private String extractSecUid(String shareUrl) {
        if (StringUtils.contains(shareUrl, "https://v.douyin.com/")) {
            throw new IllegalArgumentException("分享链接需要先解析，请使用完整用户主页链接");
        }
        if (StringUtils.contains(shareUrl, "?")) {
            return StringUtils.substringBetween(shareUrl, "https://www.douyin.com/user/", "?");
        }
        return StringUtils.substringAfterLast(shareUrl, "/");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/service/UserService.java
git commit -m "feat: 创建 UserService 用户管理服务"
```

---

## Task 6: 创建 DownloadService（核心下载逻辑）

**Files:**
- Create: `src/main/java/com/tzy/api/service/DownloadService.java`

- [ ] **Step 1: 创建 DownloadService.java - 基础结构和配置注入**

```java
package com.tzy.api.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tzy.api.common.HttpUtils;
import com.tzy.api.common.ThreadPoolUtils;
import com.tzy.api.entity.User;
import com.tzy.api.spider.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DownloadService {
    
    private static final String AID = "6383";
    private static final String ILLEGAL_CHARACTERS_REGEX = "[\\\\/:*?\"<>|\\n.]";
    private static final Pattern PATTERN = Pattern.compile("from\\_aid=(\\d+)");
    
    @Value("${app.download.base-path}")
    private String basePath;
    
    @Value("${app.download.cookie}")
    private String cookie;
    
    @Value("${app.retry.max-attempts:3}")
    private int maxAttempts;
    
    protected ExecutorService threadPool;
    
    public DownloadService() {
        this.threadPool = ThreadPoolUtils.getThreadPool(5, DownloadService.class.getSimpleName());
    }
    
    @PreDestroy
    public void close() {
        threadPool.shutdown();
    }
    
    public static String removeIllegalCharacters(String filePath) {
        if (filePath == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(ILLEGAL_CHARACTERS_REGEX);
        Matcher matcher = pattern.matcher(filePath);
        return matcher.replaceAll("");
    }
}
```

- [ ] **Step 2: 添加 HTTP 请求辅助方法**

在 `DownloadService` 类中添加：

```java
    public Map<String, String> buildHeader(String refererUrl) {
        HashMap<String, String> map = Maps.newHashMap();
        map.put("cookie", cookie);
        map.put("referer", refererUrl);
        return map;
    }
    
    private StringBuilder fill(StringBuilder builder) {
        builder.append("&channel=channel_pc_web");
        builder.append("&device_platform=webapp");
        builder.append("&browser_online=true");
        builder.append("&cookie_enabled=true");
        builder.append("&os_name=Windows");
        builder.append("&screen_width=1920");
        builder.append("&screen_height=1080");
        return builder;
    }
    
    private String removeChar(String filename) {
        return StringUtils.trim(StringUtils.replaceEach(filename, 
            new String[]{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"}, 
            new String[]{"", "", "", "", "", "", "", "", ""}));
    }
    
    private String buildFilename(AwemeList aweme) {
        String desc = removeChar(removeIllegalCharacters(aweme.getDesc()));
        String s = aweme.getCreate_time() + "-" + desc;
        if (s.length() > 200) {
            s = aweme.getAweme_id();
        }
        return s;
    }
```

- [ ] **Step 3: 添加文件操作辅助方法**

```java
    @SneakyThrows
    private void createDir(Path path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    @SneakyThrows
    private synchronized void createName(Path path, String author, String secId) {
        createDir(path);
        Path userName = Paths.get(path.toString(), StringUtils.trim(removeIllegalCharacters(author)));
        Path uid = Paths.get(path.toString(), secId);
        if (!Files.exists(userName)) {
            Files.createFile(userName);
        }
        if (!Files.exists(uid)) {
            Files.createFile(uid);
        }
    }
    
    public void addRunnable(Runnable command) {
        threadPool.execute(command);
    }
```

- [ ] **Step 4: 添加用户信息获取方法**

```java
    @SneakyThrows
    public Profile findProfile(String secUserId) {
        String s = StringUtils.substringAfter(secUserId, "/user/");
        StringBuilder stringBuilder = new StringBuilder("https://www.douyin.com/aweme/v1/web/user/profile/other/?aid=" + AID);
        stringBuilder.append("&sec_user_id=" + s);
        Document document = HttpUtils.get(stringBuilder.toString(), buildHeader(secUserId));
        String text = document.body().text();
        Profile profile = JSON.parseObject(text, Profile.class);
        if (Objects.isNull(profile)) {
            log.info(text);
        }
        return profile;
    }
    
    public String findShareFullUrl(String shareUrl) throws Exception {
        Document document = HttpUtils.get(shareUrl, buildHeader(shareUrl));
        String s = document.baseUri();
        if (StringUtils.contains(s, "www.douyin.com/user/")) {
            Matcher matcher = PATTERN.matcher(document.toString());
            if (matcher.find()) {
                String group = matcher.group();
                s = s + "&" + group + "&";
            }
        }
        return s;
    }
```

- [ ] **Step 5: 添加作品列表获取方法**

```java
    @SneakyThrows
    public JsonRootBean findPosts(Long maxCursor, String secUserId) {
        if (StringUtils.isBlank(secUserId)) {
            return null;
        }
        StringBuilder stringBuilder = fill(new StringBuilder("https://www.douyin.com/aweme/v1/web/aweme/post/?aid=" + AID));
        stringBuilder.append("&sec_user_id=" + secUserId);
        stringBuilder.append("&publish_video_strategy_type=2");
        stringBuilder.append("&from_user_page=1");
        stringBuilder.append("&version_code=290100");
        stringBuilder.append("&version_name=29.1.0");
        stringBuilder.append("&count=100");
        if (Objects.isNull(maxCursor)) {
            stringBuilder.append("&max_cursor=0");
        } else {
            stringBuilder.append("&max_cursor=").append(maxCursor);
        }
        Document document = HttpUtils.get(stringBuilder.toString(), buildHeader("https://www.douyin.com/user/" + secUserId));
        String text = document.body().text();
        try {
            return JSON.parseObject(text, JsonRootBean.class);
        } catch (Exception e) {
            log.info(text);
            if (StringUtils.contains(text, "X-TT-System-Error")) {
                Thread.sleep(TimeUnit.MINUTES.toMillis(10));
            }
            return findPosts(maxCursor, secUserId);
        }
    }
    
    public JsonRootBean findPostsByShareUrl(String shareUrl, Long maxCursor) throws Exception {
        String secUserId;
        if (StringUtils.contains(shareUrl, "https://v.douyin.com/")) {
            String code = StringUtils.substringBetween(shareUrl, "https://v.douyin.com/", "/");
            String shareFullUrl = findShareFullUrl("https://v.douyin.com/" + code + "/");
            secUserId = StringUtils.substringBefore(StringUtils.substringAfterLast(shareFullUrl, "/"), "?");
        } else {
            if (StringUtils.contains(shareUrl, "?")) {
                secUserId = StringUtils.substringBetween(shareUrl, "https://www.douyin.com/user/", "?");
            } else {
                secUserId = StringUtils.substringAfterLast(shareUrl, "/");
            }
        }
        return findPosts(maxCursor, secUserId, AID);
    }
    
    private JsonRootBean findPosts(Long maxCursor, String secUserId, String aid) {
        return findPosts(maxCursor, secUserId);
    }
```

- [ ] **Step 6: 添加视频下载方法**

```java
    private void downloadVideo(Collection<Path> paths, Video video, String filename, String shareUrl) {
        try {
            List<BitRate> bitRate = video.getBit_rate();
            if (CollectionUtils.isEmpty(bitRate)) {
                return;
            }
            ArrayList<Path> videoPaths = Lists.newArrayList();
            for (Path path : paths) {
                Path videoPath = Paths.get(path.toString() + File.separator + filename + ".mp4");
                videoPaths.add(videoPath);
            }
            Optional<String> collect = bitRate.stream()
                    .max(Comparator.comparing(BitRate::getBit_rate))
                    .map(BitRate::getPlay_addr)
                    .map(PlayAddr::getUrl_list)
                    .orElse(Lists.newArrayList())
                    .stream()
                    .filter(j -> StringUtils.contains(j, "www.douyin.com"))
                    .findFirst();
            if (collect.isPresent() && videoPaths.stream().anyMatch(o -> !Files.exists(o))) {
                Connection.Response response = HttpUtils.getResponse(collect.get(), buildHeader(shareUrl));
                byte[] bytes = response.bodyAsBytes();
                if (bytes.length <= 1024) {
                    return;
                }
                log.info("正在下载视频:{},{}", filename, paths);
                for (Path videoPath : videoPaths) {
                    if (Files.exists(videoPath)) {
                        continue;
                    }
                    Files.write(videoPath, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        } catch (Exception e) {
            log.error("downloadVideo error :{}", e.getMessage());
            downloadVideo(paths, video, filename, shareUrl);
        }
    }
```

- [ ] **Step 7: 添加图片下载方法**

```java
    private void downloadImage(AwemeList aweme, Collection<Path> paths) {
        try {
            List<Images> images = aweme.getImages();
            if (CollectionUtils.isEmpty(images)) {
                return;
            }
            String desc = removeChar(removeIllegalCharacters(aweme.getDesc()));
            ArrayList<Path> imagesPaths = Lists.newArrayList();
            for (Path path : paths) {
                String s = aweme.getCreate_time() + "-" + desc;
                if (s.length() > 200) {
                    s = aweme.getAweme_id();
                }
                Path imagesPath = Paths.get(path.toString() + File.separator + s);
                createDir(imagesPath);
                imagesPaths.add(imagesPath);
            }

            if (CollectionUtils.isNotEmpty(images)) {
                for (Path imagesPath : imagesPaths) {
                    for (int i = 0, imagesSize = images.size(); i < imagesSize; i++) {
                        Images image = images.get(i);
                        Optional<String> first = image.getUrl_list().stream().filter(o -> StringUtils.contains(o, ".jpeg?")).findFirst();
                        Path imagePath = Paths.get(imagesPath.toString(), i + ".jpeg");
                        if (!Files.exists(imagePath) && first.isPresent()) {
                            Connection.Response response = HttpUtils.getResponse(first.get());
                            byte[] bytes = response.bodyAsBytes();
                            log.info("正在下载图片:{},{}", desc, imagePath);
                            Files.write(imagePath, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                        }
                        if (Objects.nonNull(image.getVideo())) {
                            downloadVideo(Stream.of(imagesPath).collect(Collectors.toSet()), image.getVideo(), String.valueOf(i), aweme.getShare_url());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("downloadImage error :{}", e.getMessage());
            downloadImage(aweme, paths);
        }
    }
```

- [ ] **Step 8: 添加核心下载方法（带重试）**

```java
    private void doDownload(AwemeList o) {
        addRunnable(() -> {
            Author author = o.getAuthor();
            ArrayList<Path> paths = Lists.newArrayList();
            CooperationInfo partners = o.getCooperation_info();
            Path path = Paths.get(basePath, author.getUid());
            createName(path, author.getNickname(), author.getSec_uid());
            if (Objects.nonNull(partners)) {
                List<CoCreators> partnerList = partners.getCo_creators();
                if (CollectionUtils.isNotEmpty(partnerList)) {
                    for (CoCreators co_creator : partnerList) {
                        Path path1 = Paths.get(basePath, co_creator.getUid());
                        createName(path1, co_creator.getNickname(), co_creator.getSec_uid());
                        paths.add(path1);
                    }
                }
            }
            downloadImage(o, path, paths);
            downloadVideo(o, path, paths);
        });
    }
    
    private void downloadVideo(AwemeList aweme, Collection<Path> paths) {
        downloadVideo(paths, aweme.getVideo(), buildFilename(aweme), aweme.getShare_url());
    }
    
    private void downloadVideo(AwemeList aweme, Path main, Collection<Path> partners) {
        downloadVideo(main, partners, aweme.getVideo(), buildFilename(aweme), aweme.getShare_url());
    }
    
    private void downloadVideo(Path main, Collection<Path> partners, Video video, String filename, String shareUrl) {
        downloadVideo(Lists.newArrayList(main), video, filename, shareUrl);
    }
```

- [ ] **Step 9: 添加用户内容下载方法**

```java
    public void downloadUserContent(User user, boolean update) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
                findContentWithHandle(user.getShareUrl(), 0L, this::doDownload, update);
                return;
            } catch (Exception e) {
                attempts++;
                log.warn("下载失败，第 {} 次重试，用户: {}", attempts, user.getNickname());
                if (attempts >= maxAttempts) {
                    log.error("达到最大重试次数，跳过用户: {}", user.getNickname());
                }
            }
        }
    }
    
    public void findContentWithHandle(String shareUrl, Long maxCursor, Consumer<AwemeList> handle, boolean update) {
        log.info("正在处理用户数据：{}", shareUrl);
        try {
            JsonRootBean jsonRootBean = findPostsByShareUrl(shareUrl, maxCursor);
            if (Objects.isNull(jsonRootBean)) {
                log.warn("RESPONSE_IS_NULL: {} ,{}", maxCursor, shareUrl);
                findContentWithHandle(shareUrl, maxCursor, handle, update);
                return;
            }
            Long status_code = jsonRootBean.getStatus_code();
            Long has_more = jsonRootBean.getHas_more();
            if (status_code != 0) {
                log.warn("STATUS_CODE_IS_NOT_ZERO: {} ,{}", maxCursor, shareUrl);
                findContentWithHandle(shareUrl, maxCursor, handle, update);
                return;
            }
            List<AwemeList> list = jsonRootBean.getAweme_list();
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            int total = list.size();
            int current = list.size();
            if (update) {
                Instant minus = Instant.now().minus(5L, ChronoUnit.DAYS);
                list = list.stream()
                        .filter(o -> Instant.ofEpochSecond(o.getCreate_time()).isAfter(minus))
                        .collect(Collectors.toList());
                current = list.size();
                if (CollectionUtils.isEmpty(list)) {
                    return;
                }
            }
            for (AwemeList awemeList : list) {
                doDownload(awemeList);
            }
            if (has_more == 1 && total == current) {
                findContentWithHandle(shareUrl, jsonRootBean.getMax_cursor(), handle, update);
            }
        } catch (Exception e) {
            log.error("findContentWithHandle error", e);
            findContentWithHandle(shareUrl, maxCursor, handle, update);
        }
    }
```

- [ ] **Step 10: 添加收藏夹发现方法**

```java
    @SneakyThrows
    public List<Author> discoverFromCollects(Long collectsId) {
        List<Author> newAuthors = Lists.newArrayList();
        discoverFromCollects(0L, collectsId, newAuthors);
        return newAuthors;
    }
    
    @SneakyThrows
    private void discoverFromCollects(Long maxCursor, Long collectsId, List<Author> newAuthors) {
        StringBuilder stringBuilder = fill(new StringBuilder("https://www.douyin.com/aweme/v1/web/collects/video/list/?"));
        stringBuilder.append("&aid=" + AID);
        stringBuilder.append("&collects_id=" + collectsId);
        stringBuilder.append("&cursor=" + maxCursor);
        stringBuilder.append("&count=" + 100);
        stringBuilder.append("&update_version_code=170400");
        stringBuilder.append("&pc_client_type=1");
        stringBuilder.append("&pc_libra_divert=Windows");
        stringBuilder.append("&support_h265=1");
        stringBuilder.append("&support_dash=1");
        stringBuilder.append("&version_code=170400");
        stringBuilder.append("&version_name=17.4.0");
        stringBuilder.append("&browser_language=zh-CN");
        stringBuilder.append("&browser_platform=Win32");
        stringBuilder.append("&browser_name=Edge");
        stringBuilder.append("&browser_version=135.0.0.0");
        stringBuilder.append("&engine_name=Blink");
        stringBuilder.append("&engine_version=135.0.0.0");
        stringBuilder.append("&os_version=10");
        stringBuilder.append("&cpu_core_num=4");
        stringBuilder.append("&device_memory=8");
        stringBuilder.append("&platform=PC");
        stringBuilder.append("&downlink=10");
        stringBuilder.append("&effective_type=4g");
        stringBuilder.append("&round_trip_time=50");

        Document document = HttpUtils.get(stringBuilder.toString(), buildHeader("https://www.douyin.com/user/self?from_tab_name=main&showSubTab=favorite_folder&showTab=favorite_collection"));
        Collect collect = JSON.parseObject(document.body().text(), Collect.class);
        List<AwemeList> followings = collect.getAweme_list();
        if (CollectionUtils.isEmpty(followings)) {
            return;
        }
        for (AwemeList follow : followings) {
            Author author = follow.getAuthor();
            newAuthors.add(author);
        }
        if (Objects.equals(1, collect.getHas_more())) {
            discoverFromCollects(collect.getCursor(), collectsId, newAuthors);
        }
    }
```

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/tzy/api/service/DownloadService.java
git commit -m "feat: 创建 DownloadService 下载核心逻辑"
```

---

## Task 7: 创建 UserController

**Files:**
- Create: `src/main/java/com/tzy/api/controller/UserController.java`

- [ ] **Step 1: 创建 UserController.java**

```java
package com.tzy.api.controller;

import com.tzy.api.entity.User;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public List<User> listUsers() {
        return userService.findAll();
    }
    
    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody AddUserRequest request) {
        User user = userService.addUser(request.getShareUrl(), request.getNickname());
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        User user = userService.updateEnabled(id, request.getEnabled());
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @lombok.Data
    public static class AddUserRequest {
        private String shareUrl;
        private String nickname;
    }
    
    @lombok.Data
    public static class UpdateUserRequest {
        private Boolean enabled;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/controller/UserController.java
git commit -m "feat: 创建 UserController REST API"
```

---

## Task 8: 创建 TaskController

**Files:**
- Create: `src/main/java/com/tzy/api/controller/TaskController.java`

- [ ] **Step 1: 创建 TaskController.java**

```java
package com.tzy.api.controller;

import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final DownloadService downloadService;
    private final UserService userService;
    private final AtomicBoolean taskRunning = new AtomicBoolean(false);

    @PostMapping("/start")
    public String startDownloadTask() {
        if (taskRunning.get()) {
            return "任务正在运行中，请稍后再试";
        }
        new Thread(() -> {
            taskRunning.set(true);
            try {
                log.info("手动触发下载任务开始");
                userService.findEnabledUsers().forEach(user -> {
                    downloadService.downloadUserContent(user);
                });
                log.info("手动触发下载任务完成");
            } finally {
                taskRunning.set(false);
            }
        }).start();
        return "下载任务已启动";
    }

    @GetMapping("/status")
    public String getTaskStatus() {
        return taskRunning.get() ? "RUNNING" : "IDLE";
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/controller/TaskController.java
git commit -m "feat: 创建 TaskController 任务管理 API"
```

---

## Task 9: 创建 ScheduleConfig

**Files:**
- Create: `src/main/java/com/tzy/api/config/ScheduleConfig.java`

- [ ] **Step 1: 创建 ScheduleConfig.java**

```java
package com.tzy.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ScheduleConfig {
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/config/ScheduleConfig.java
git commit -m "feat: 创建 ScheduleConfig 启用定时任务"
```

---

## Task 10: 创建 DownloadScheduler

**Files:**
- Create: `src/main/java/com/tzy/api/scheduler/DownloadScheduler.java`

- [ ] **Step 1: 创建 DownloadScheduler.java**

```java
package com.tzy.api.scheduler;

import com.tzy.api.entity.User;
import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadScheduler {

    private final DownloadService downloadService;
    private final UserService userService;

    @Scheduled(cron = "${app.schedule.download-cron}")
    public void executeDownloadTask() {
        log.info("定时下载任务开始");
        userService.findEnabledUsers().forEach(user -> {
            downloadService.downloadUserContent(user);
        });
        log.info("定时下载任务完成");
    }

    @Scheduled(cron = "${app.schedule.collects-cron}")
    public void executeCollectsDiscovery() {
        log.info("收藏夹发现任务开始");
        Long collectsId = 7492594126609848114L;
        List<User> existingUsers = userService.findAll();
        Set<String> existingSecUids = existingUsers.stream()
                .map(User::getSecUid)
                .collect(Collectors.toSet());

        downloadService.discoverFromCollects(collectsId).forEach(author -> {
            if (!existingSecUids.contains(author.getSec_uid())) {
                String shareUrl = "http://www.douyin.com/user/" + author.getSec_uid();
                userService.addUserIfNotExists(shareUrl, author.getNickname());
                log.info("发现新用户: {}", author.getNickname());
            }
        });
        log.info("收藏夹发现任务完成");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/scheduler/DownloadScheduler.java
git commit -m "feat: 创建 DownloadScheduler 定时任务调度器"
```

---

## Task 11: 更新主启动类

**Files:**
- Modify: `src/main/java/com/tzy/api/DouyinDownloadApplication.java`

- [ ] **Step 1: 修改 DouyinDownloadApplication.java**

```java
package com.tzy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DouyinDownloadApplication {
    public static void main(String[] args) {
        SpringApplication.run(DouyinDownloadApplication.class, args);
    }
}
```

（注：ScheduleConfig 已通过 @Configuration 注解自动扫描，主类无需额外修改）

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/tzy/api/DouyinDownloadApplication.java
git commit -m "refactor: 更新主启动类"
```

---

## Task 12: 添加 GraalVM 反射配置

**Files:**
- Create: `src/main/resources/META-INF/native-image/reflect-config.json`

- [ ] **Step 1: 创建 reflect-config.json**

```json
[
  {
    "name": "com.tzy.api.entity.User",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.JsonRootBean",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.AwemeList",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.Author",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.Video",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.Images",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.Profile",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  },
  {
    "name": "com.tzy.api.spider.dto.Collect",
    "allDeclaredConstructors": true,
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  }
]
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/META-INF/native-image/reflect-config.json
git commit -m "config: 添加 GraalVM Native Image 反射配置"
```

---

## Task 13: 更新 AGENTS.md

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 更新 AGENTS.md**

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add AGENTS.md
git commit -m "docs: 更新 AGENTS.md"
```

---

## Task 14: 验证构建

**Files:**
- None

- [ ] **Step 1: 编译验证**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: 打包验证**

Run: `mvn clean package -DskipTests`
Expected: BUILD SUCCESS，生成 target/douyin-download-1.0.0.jar

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "feat: 完成 Spring Boot 驱动改造"
```
