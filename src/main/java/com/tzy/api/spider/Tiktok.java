package com.tzy.api.spider;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tzy.api.common.HttpUtils;
import com.tzy.api.common.ThreadPoolUtils;
import com.tzy.api.spider.dto.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Tiktok {
    public static final String RECORD_FILE = "E:\\1.txt";
    static final String FOLLOW_USER_ID = "95469136501";
    static final String FOLLOW_SEC_UID = "MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc";
    private static final Pattern pattern = Pattern.compile("from\\_aid=(\\d+)");
    private static final String NEW_BASE_PATH = "F:\\douyin";
    private static final String AID = "6383";
    /**
     * 最后兜底：内置旧 cookie（已过期，仅在 -Ddouyin.cookie 和 cookies.txt 都缺失时使用）。
     */
    private static final String FALLBACK_COOKIE = loadFallbackCookie();
    private static final Pattern ILLEGAL_PATTERN = Pattern.compile("[\\/:*?\"<>|\\n\\.]");
    private static final Object RELOGIN_LOCK = new Object();
    private static final String BASE_PATH = "E:\\douyin";
    private static final Object LOGIN_CHECK_LOCK = new Object();
    private static final long LOGIN_CHECK_INTERVAL_MS = 10 * 60_000L;
    private static final long LOGIN_RECHECK_INTERVAL_MS = 60_000L;
    /**
     * 优先级：-Ddouyin.cookie > cookies.txt > 内置旧 cookie。
     */
    static volatile String COOKIE = resolveCookie();
    private static volatile long lastReloginAt = 0L;
    private static volatile long lastLoginCheckAt = 0L;
    protected ExecutorService threadPool;
    private DouyinBrowserClient browserClient;

    public Tiktok() {
        this.threadPool = ThreadPoolUtils.getThreadPool(5, Tiktok.class.getSimpleName());
    }

    private static String loadFallbackCookie() {
        try (InputStream in = Tiktok.class.getResourceAsStream("/legacy-cookie.txt")) {
            return in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private static String resolveCookie() {
        String inline = System.getProperty("douyin.cookie");
        if (inline != null && !inline.isBlank()) {
            return inline;
        }
        Path file = Path.of(System.getProperty("douyin.cookieFile", "cookies.txt"));
        try {
            if (Files.isRegularFile(file)) {
                String value = Files.readString(file).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        } catch (IOException ignored) {
            // 读取失败时回退到内置 cookie
        }
        return FALLBACK_COOKIE;
    }

    public static String removeIllegalCharacters(String filePath) {
        return filePath == null ? null : ILLEGAL_PATTERN.matcher(filePath).replaceAll("");
    }

    public static void main(String[] args) {
        Tiktok tiktok = new Tiktok();
        tiktok.findCollects(0L, 7492594126609848114L);
        tiktok.jobList(true);
//        tiktok.confirm();
        tiktok.close();
    }

    @SneakyThrows
    private static void delete(DirectoryStream<Path> basePath) {
        for (Path path : basePath) {
            if (!Files.isDirectory(path)) {
                Files.delete(path);
            } else {
                DirectoryStream<Path> childPath = Files.newDirectoryStream(path);
                if (!childPath.iterator().hasNext()) {
                    Files.delete(path);
                } else {
                    delete(Files.newDirectoryStream(path));
                }
                Files.delete(path);
            }
        }
    }

    @SneakyThrows
    private void confirm() {
        ArrayList<String> filePaths = Lists.newArrayList("E:\\1.txt");
        Set<String> strings = Sets.newHashSet();
        for (String filePath : filePaths) {
            strings.addAll(Files.readAllLines(Paths.get(filePath)));
        }
        for (String string : strings) {
            // 每 10 分钟用关注接口探活，登录失效时在这里自动重新登录
            ensureLogin();
            String http = StringUtils.substringAfter(string, "http");
            Profile profile = browserClient().findProfile("http" + http);
            if ((Objects.isNull(profile) || Objects.isNull(profile.getUser())
                    || Objects.isNull(profile.getUser().getAweme_count()))
                    && reloginIfLoginInvalid()) {
                profile = browserClient().findProfile("http" + http);
            }
            boolean error = true;
            Integer aweme_count = 0;
            if (Objects.isNull(profile) || Objects.isNull(profile.getUser())) {
                error = false;
            } else {
                Integer aweme_count1 = profile.getUser().getAweme_count();
                if (Objects.nonNull(aweme_count1)) {
                    aweme_count = aweme_count1;
                } else {
                    error = false;
                }
            }
            if (error && aweme_count > 0) {
                Files.write(Paths.get("E:\\2.txt"), (string + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } else {
                System.out.println(string);
                Files.write(Paths.get("E:\\3.txt"), (string + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
        }
    }

    @SneakyThrows
    private void cancelFollow() {
        Set<String> recorded = allLine();
        ArrayList<Followings> followings = findAllMyFollow(0L);
        browserClient().cancelFollow(recorded, followings);
    }

    private void deleteDir(DirectoryStream<Path> basePath) throws IOException {
        if (Objects.isNull(basePath)) {
            basePath = Files.newDirectoryStream(Paths.get(BASE_PATH));
        }
        for (Path path : basePath) {
            String dirName = path.getName(path.getNameCount() - 1).toString();
            if (StringUtils.startsWith(dirName, "UID")) {
                continue;
            }
            if (Files.isDirectory(path)) {
                ArrayList<Path> paths1 = Lists.newArrayList(Files.newDirectoryStream(path));
                if (CollectionUtils.isEmpty(paths1)) {
                    System.out.println(path.toString());
                    Files.delete(path);
                } else {
                    for (Path path1 : paths1) {
                        if (Files.isDirectory(path1)) {
                            ArrayList<Path> paths = Lists.newArrayList(Files.newDirectoryStream(path1));
                            if (CollectionUtils.isEmpty(paths)) {
                                System.out.println(path1);
                                Files.delete(path1);
                            } else {
                                deleteDir(Files.newDirectoryStream(path1));
                            }
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    private void move(DirectoryStream<Path> basePath) {
        if (Objects.isNull(basePath)) {
            basePath = Files.newDirectoryStream(Paths.get(BASE_PATH));
        }
        for (Path path : basePath) {
            String dirName = path.getName(path.getNameCount() - 1).toString();
            if (StringUtils.startsWith(dirName, "UID")) {
                continue;
            }
            if (Files.isDirectory(path)) {
                move(Files.newDirectoryStream(path));
            } else {
                long threeMonth = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
                long l = path.toFile().lastModified();
                if (l > threeMonth) {
                    continue;
                }

                addRunnable(() -> {
                    try {
                        System.out.println(path);
                        String destPath = StringUtils.remove(path.toString(), BASE_PATH);
                        destPath = NEW_BASE_PATH + StringUtils.remove(destPath, path.getFileName().toString());
                        System.out.println(destPath);
                        Path toPath = Paths.get(destPath);
                        createDir(toPath);
                        toPath = Paths.get(toPath.toString(), path.getFileName().toString());
                        if (!Files.exists(toPath)) {
                            if (StringUtils.contains(toPath.toString(), ".")) {
                                Files.move(path, toPath, StandardCopyOption.REPLACE_EXISTING);
                            } else {
                                Files.copy(path, toPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    @SneakyThrows
    private void repeat(DirectoryStream<Path> paths) {
        if (Objects.isNull(paths)) {
            paths = Files.newDirectoryStream(Paths.get(BASE_PATH));
        }
        HashMap<String, Path> map = Maps.newHashMap();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                File file = path.toFile();
                String name = file.getName();
                if (StringUtils.startsWith(name, "UID")) {
                    continue;
                }
                repeat(Files.newDirectoryStream(path));
            } else {
                String name = path.toFile().getName();
                String s = StringUtils.substringBefore(name, "-");
                if (map.containsKey(s)) {
                    Path existPath = map.get(s);
                    long existLastModifiedTime = Files.getLastModifiedTime(existPath).toInstant().toEpochMilli();
                    long currentLastModifiedTime = Files.getLastModifiedTime(path).toInstant().toEpochMilli();
                    if (currentLastModifiedTime > existLastModifiedTime) {
                        Files.delete(existPath);
                    } else {
                        Files.delete(path);
                    }
                } else {
                    map.put(s, path);
                }
            }
        }
    }

    @SneakyThrows
    private void clean(DirectoryStream<Path> paths) {
        Set<String> strings = allLine();
        if (Objects.isNull(paths)) {
            paths = Files.newDirectoryStream(Paths.get(BASE_PATH));
        }
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                File file = path.toFile();
                String name = file.getName();
                if (StringUtils.startsWith(name, "UID")) {
                    continue;
                }
                DirectoryStream<Path> paths1 = Files.newDirectoryStream(path);
                ArrayList<Path> list = Lists.newArrayList(paths1.iterator());
                Set<String> collect = list.stream()
                        .filter(o -> !Files.isDirectory(path))
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(o -> !StringUtils.contains(o, "."))
                        .filter(o -> !StringUtils.contains(o, "-"))
                        .collect(Collectors.toSet());
                if (strings.stream().noneMatch(o -> collect.stream().anyMatch(j -> StringUtils.contains(o, j)))) {
                    System.out.println(path);
                    if (list.size() < 12) {
                        delete(Files.newDirectoryStream(path));
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void jobList(boolean update) {
        for (String url : allLine()) {
            job(url, 0L, update);
        }
    }

    @SneakyThrows
    private Set<String> allLine() {
        return Sets.newHashSet(Files.readAllLines(Paths.get(RECORD_FILE)));
    }

    public void close() {
        threadPool.shutdown();
        synchronized (this) {
            if (Objects.nonNull(browserClient)) {
                browserClient.close();
                browserClient = null;
            }
        }
    }

    public void job(String shareUrl, Long maxCursor, boolean update) {
        findContentWithHandle(shareUrl, maxCursor, this::doDownload, update);
    }

    private void doDownload(AwemeList o) {
        addRunnable(() -> {
            Author author = o.getAuthor();
            ArrayList<Path> paths = Lists.newArrayList();
            CooperationInfo partners = o.getCooperation_info();
            Path path = Paths.get(BASE_PATH, author.getUid());
            createName(path, author.getNickname(), author.getSec_uid());
            if (Objects.nonNull(partners)) {
                List<CoCreators> partnerList = partners.getCo_creators();
                if (CollectionUtils.isNotEmpty(partnerList)) {
                    for (CoCreators co_creator : partnerList) {
                        Path path1 = Paths.get(BASE_PATH, co_creator.getUid());
                        createName(path1, co_creator.getNickname(), co_creator.getSec_uid());
                        paths.add(path1);
                    }
                }
            }
            downloadImage(o, path, paths);
            downloadVideo(o, path, paths);
        });
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

    public List<AwemeList> findAllContent(String shareUrl, Long maxCursor) {
        return findAllContent(shareUrl, maxCursor, 0);
    }

    private List<AwemeList> findAllContent(String shareUrl, Long maxCursor, int attempt) {
        List<AwemeList> result = Lists.newArrayList();
        JsonRootBean jsonRootBean;
        try {
            jsonRootBean = findPostsByShareUrl(shareUrl, maxCursor);
        } catch (Exception e) {
            log.error("findPostsByShareUrl error, attempt={}", attempt, e);
            return attempt < 3 ? findAllContent(shareUrl, maxCursor, attempt + 1) : result;
        }
        if (Objects.isNull(jsonRootBean)) {
            log.warn("RESPONSE_IS_NULL:{},{}", maxCursor, shareUrl);
            return attempt < 3 ? findAllContent(shareUrl, maxCursor, attempt + 1) : result;
        }
        if (!Objects.equals(jsonRootBean.getStatus_code(), 0L)) {
            log.warn("STATUS_CODE_IS_NOT_ZERO:{},{}", jsonRootBean.getStatus_code(), shareUrl);
            return attempt < 3 ? findAllContent(shareUrl, maxCursor, attempt + 1) : result;
        }
        if (CollectionUtils.isNotEmpty(jsonRootBean.getAweme_list())) {
            result.addAll(jsonRootBean.getAweme_list());
        }
        if (Objects.equals(jsonRootBean.getHas_more(), 1L)) {
            result.addAll(findAllContent(shareUrl, jsonRootBean.getMax_cursor(), 0));
        }
        return result;
    }

    public void findContentWithHandle(String shareUrl, Long maxCursor, Consumer<AwemeList> handle, boolean update) {
        findContentWithHandle(shareUrl, maxCursor, handle, update, 0);
    }

    private void findContentWithHandle(String shareUrl, Long maxCursor, Consumer<AwemeList> handle, boolean update, int attempt) {
        log.info("正在处理登记数据：{}", shareUrl);
        try {
            JsonRootBean jsonRootBean = findPostsByShareUrl(shareUrl, maxCursor);
            if (Objects.isNull(jsonRootBean)) {
                log.warn("RESPONSE_IS_NULL: {} ,{}", maxCursor, shareUrl);
                return;
            }
            if (!Objects.equals(jsonRootBean.getStatus_code(), 0L)) {
                log.warn("STATUS_CODE_IS_NOT_ZERO: {} ,{}", maxCursor, shareUrl);
                if (attempt < 3) {
                    findContentWithHandle(shareUrl, maxCursor, handle, update, attempt + 1);
                }
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
            if (Objects.equals(jsonRootBean.getHas_more(), 1L) && total == current) {
                findContentWithHandle(shareUrl, jsonRootBean.getMax_cursor(), handle, update, 0);
            }
        } catch (Exception e) {
            log.error("findContentWithHandle error, attempt={}", attempt, e);
            if (attempt < 3) {
                findContentWithHandle(shareUrl, maxCursor, handle, update, attempt + 1);
            }
        }
    }

    public List<AwemeList> findUpdatesByFollow(Long maxCursor) {
        List<AwemeList> result = Lists.newArrayList();
        ArrayList<Followings> myFollow = findAllMyFollow(0L);
        List<String> secUidList = myFollow.stream()
                .filter(o -> CollectionUtils.isNotEmpty(o.getNot_seen_item_id_list_v2()))
                .map(Followings::getSec_uid)
                .collect(Collectors.toList());
        for (String secUid : secUidList) {
            result.addAll(findAllContent(maxCursor, secUid));
        }
        return result;
    }

    private List<AwemeList> findAllContent(Long maxCursor, String secUid) {
        return findAllContent(maxCursor, secUid, 0);
    }

    private List<AwemeList> findAllContent(Long maxCursor, String secUid, int attempt) {
        List<AwemeList> result = Lists.newArrayList();
        JsonRootBean jsonRootBean = findPosts(maxCursor, secUid, AID);
        if (Objects.isNull(jsonRootBean) || !Objects.equals(jsonRootBean.getStatus_code(), 0L)) {
            return attempt < 3 ? findAllContent(maxCursor, secUid, attempt + 1) : result;
        }
        if (CollectionUtils.isNotEmpty(jsonRootBean.getAweme_list())) {
            result.addAll(jsonRootBean.getAweme_list());
        }
        if (Objects.equals(jsonRootBean.getHas_more(), 1L)) {
            result.addAll(findAllContent(jsonRootBean.getMax_cursor(), secUid, 0));
        }
        return result;
    }

    public void addRunnable(Runnable command) {
        threadPool.execute(command);
    }

    @SneakyThrows
    private void createDir(List<Path> paths) {
        for (Path path : paths) {
            createDir(path);
        }
    }

    @SneakyThrows
    private void createDir(Path path) {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void downloadVideo(AwemeList aweme, Collection<Path> paths) {
        downloadVideo(paths, aweme.getVideo(), buildFilename(aweme), aweme.getShare_url());
    }

    private void downloadVideo(AwemeList aweme, Path main, Collection<Path> partners) {
        downloadVideo(main, partners, aweme.getVideo(), buildFilename(aweme), aweme.getShare_url());
    }

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

    private void downloadVideo(Path main, Collection<Path> partners, Video video, String filename, String shareUrl) {
        try {
            String s = File.separator + filename + ".mp4";
            Path mainVideoPath = Paths.get(main.toString() + s);
            ArrayList<Path> videoPaths = Lists.newArrayList();
            for (Path path : partners) {
                Path videoPath = Paths.get(path.toString() + s);
                videoPaths.add(videoPath);
            }
            Optional<String> collect = video.getPlay_addr().getUrl_list().stream().filter(j -> StringUtils.contains(j, "www.douyin.com")).findFirst();
            if (collect.isPresent() && !Files.exists(mainVideoPath)) {
                Connection.Response response = HttpUtils.getResponse(collect.get(), buildHeader(shareUrl));
                byte[] bytes = response.bodyAsBytes();
                if (bytes.length <= 1024) {
                    return;
                }
                log.info("正在下载视频:{},{}", filename, main);
                Files.write(mainVideoPath, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            if (collect.isPresent() && videoPaths.stream().anyMatch(o -> !Files.exists(o))) {
                log.info("正在下载视频:{},{}", filename, partners);
                for (Path videoPath : videoPaths) {
                    if (Files.exists(videoPath)) {
                        continue;
                    }
                    Files.createLink(videoPath, mainVideoPath);
                }
            }
        } catch (Exception e) {
            log.error("downloadVideo error :{}", e.getMessage());
            downloadVideo(main, partners, video, filename, shareUrl);
        }
    }

    private String buildFilename(AwemeList aweme) {
        String desc = removeChar(removeIllegalCharacters(aweme.getDesc()));
        String s = aweme.getCreate_time() + "-" + desc;
        if (s.length() > 200) {
            s = aweme.getAweme_id();
        }
        return s;
    }

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

    private void downloadImage(AwemeList aweme, Path main, Collection<Path> partners) {
        try {
            List<Images> images = aweme.getImages();
            if (CollectionUtils.isEmpty(images)) {
                return;
            }
            String desc = removeChar(removeIllegalCharacters(aweme.getDesc()));
            String folderName = aweme.getCreate_time() + "-" + desc;
            if (folderName.length() > 200) {
                folderName = aweme.getAweme_id();
            }
            Path mainImagePath = Paths.get(main.toString() + File.separator + folderName);
            createDir(mainImagePath);
            for (int i = 0, imagesSize = images.size(); i < imagesSize; i++) {
                Images image = images.get(i);
                Optional<String> first = image.getUrl_list().stream().filter(o -> StringUtils.contains(o, ".jpeg?")).findFirst();
                Path imagePath = Paths.get(mainImagePath.toString(), i + ".jpeg");
                if (!Files.exists(imagePath) && first.isPresent()) {
                    Connection.Response response = HttpUtils.getResponse(first.get());
                    byte[] bytes = response.bodyAsBytes();
                    log.info("正在下载图片:{},{}", desc, imagePath);
                    Files.write(imagePath, bytes, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                }
                if (Objects.nonNull(image.getVideo())) {
                    downloadVideo(mainImagePath, Lists.newArrayList(), image.getVideo(), String.valueOf(i), aweme.getShare_url());
                }
            }
            ArrayList<Path> imagesPaths = Lists.newArrayList();
            for (Path path : partners) {
                Path imagesPath = Paths.get(path.toString() + File.separator + folderName);
                createDir(imagesPath);
                imagesPaths.add(imagesPath);
            }

            if (CollectionUtils.isNotEmpty(images)) {
                for (Path imagesPath : imagesPaths) {
                    for (int i = 0, imagesSize = images.size(); i < imagesSize; i++) {
                        Images image = images.get(i);
                        String filename = i + ".jpeg";
                        Optional<String> first = image.getUrl_list().stream().filter(o -> StringUtils.contains(o, ".jpeg?")).findFirst();
                        Path imagePath = Paths.get(imagesPath.toString(), filename);
                        if (!Files.exists(imagePath) && first.isPresent()) {
                            Files.createLink(imagePath, Paths.get(mainImagePath.toString(), filename));
                        }
                        if (Objects.nonNull(image.getVideo())) {
                            downloadVideo(mainImagePath, Stream.of(imagesPath).collect(Collectors.toSet()), image.getVideo(), String.valueOf(i), aweme.getShare_url());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("downloadImage error :{}", e.getMessage());
            downloadImage(aweme, main, partners);
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

    public JsonRootBean findPosts(Long maxCursor, String secUserId, String aid) {
        if (StringUtils.isBlank(secUserId)) {
            return null;
        }
        ensureLogin();
        String finalAid = StringUtils.isBlank(aid) ? AID : aid;
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;

        JsonRootBean bean = findPostsWithRetry(cursor, secUserId, finalAid);
        if (Objects.nonNull(bean)) {
            return bean;
        }
        log.warn("findPosts 连续失败，尝试重新登录后重试, secUserId={}, cursor={}", secUserId, cursor);
        if (reLogin()) {
            lastLoginCheckAt = 0L;
            bean = findPostsWithRetry(cursor, secUserId, finalAid);
        }
        return bean;
    }

    private JsonRootBean findPostsWithRetry(long cursor, String secUserId, String aid) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonRootBean bean = browserClient().findPosts(cursor, secUserId, aid);
                if (Objects.nonNull(bean) && Objects.equals(bean.getStatus_code(), 0L)) {
                    return bean;
                }
                log.warn("findPosts status={} (attempt {}/3), secUserId={}, cursor={}",
                        Objects.isNull(bean) ? "null" : bean.getStatus_code(), attempt, secUserId, cursor);
            } catch (Exception e) {
                log.warn("findPosts error (attempt {}/3), secUserId={}, cursor={}: {}",
                        attempt, secUserId, cursor, e.getMessage());
            }
            if (attempt < 3) {
                try {
                    Thread.sleep(1_000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private synchronized DouyinBrowserClient browserClient() {
        if (Objects.isNull(browserClient) || browserClient.isClosed()) {
            // 保留登录 cookie，但丢弃可能被风控的 ttwid，由页面重新生成；
            // 旧 ttwid 会导致作品接口返回 HTTP 200 + 空 body
            browserClient = new DouyinBrowserClient(COOKIE, true);
        }
        return browserClient;
    }

    /**
     * 作品接口未登录时不会报错，只会少返回内容，因此每 10 分钟用关注接口确认一次登录态。
     */
    private void ensureLogin() {
        if (System.currentTimeMillis() - lastLoginCheckAt < LOGIN_CHECK_INTERVAL_MS) {
            return;
        }
        synchronized (LOGIN_CHECK_LOCK) {
            if (System.currentTimeMillis() - lastLoginCheckAt < LOGIN_CHECK_INTERVAL_MS) {
                return;
            }
            try {
                MyFollow check = browserClient().findMyFollow(0L, FOLLOW_USER_ID, FOLLOW_SEC_UID);
                if (Objects.isNull(check) || !Objects.equals(check.getStatus_code(), 0L)) {
                    log.warn("作品抓取前登录态检查失败 status={}，尝试重新登录",
                            Objects.isNull(check) ? "null" : check.getStatus_code());
                    reLogin();
                }
            } catch (Exception e) {
                log.warn("作品抓取前登录态检查异常: {}", e.getMessage());
            } finally {
                lastLoginCheckAt = System.currentTimeMillis();
            }
        }
    }

    /**
     * profile 空响应时调用：先用关注接口确认登录态，确实失效才重新登录。
     * 1 分钟内最多探活一次，避免空响应逐条触发。
     *
     * @return true 表示刚完成重新登录
     */
    private boolean reloginIfLoginInvalid() {
        if (System.currentTimeMillis() - lastLoginCheckAt < LOGIN_RECHECK_INTERVAL_MS) {
            return false;
        }
        synchronized (LOGIN_CHECK_LOCK) {
            if (System.currentTimeMillis() - lastLoginCheckAt < LOGIN_RECHECK_INTERVAL_MS) {
                return false;
            }
            try {
                return reLogin();
            } catch (Exception e) {
                log.warn("登录态检查异常: {}", e.getMessage());
                return false;
            } finally {
                lastLoginCheckAt = System.currentTimeMillis();
            }
        }
    }

    /**
     * 登录接口返回 status=8/4 时，打开浏览器让用户重新登录，成功后替换 COOKIE 并重试原请求。
     * 5 分钟内只尝试一次，避免多线程反复弹窗。
     */
    private boolean reLogin() {
        synchronized (RELOGIN_LOCK) {
            long now = System.currentTimeMillis();
            if (now - lastReloginAt < 5 * 60_000L) {
                log.warn("5 分钟内已尝试过重新登录，跳过");
                return false;
            }
            synchronized (this) {
                // 作品/关注/收藏共用一个浏览器，重新登录后必须重建才能用上新 cookie
                if (Objects.nonNull(browserClient)) {
                    browserClient.close();
                    browserClient = null;
                }
            }
            log.warn("登录态失效，打开浏览器等待重新登录（最长 10 分钟）...");
            Path dir = LoginCapture.workDir();
            if (!LoginCapture.loginAndExport(dir, 0L)) {
                log.warn("重新登录未完成，继续使用旧登录态");
                return false;
            }
            try {
                COOKIE = Files.readString(dir.resolve("cookies.txt"), StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                log.warn("读取新 cookie 失败: {}", e.getMessage());
                return false;
            }
            lastReloginAt = System.currentTimeMillis();
            log.info("重新登录成功，继续执行");
            return true;
        }
    }

    private ArrayList<Followings> findAllMyFollow(Long maxCursor) {
        return findAllMyFollow(maxCursor, 0, true);
    }

    private ArrayList<Followings> findAllMyFollow(Long maxCursor, int attempt, boolean allowRelogin) {
        ArrayList<Followings> result = Lists.newArrayList();
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;
        try {
            MyFollow myFollow = browserClient().findMyFollow(cursor, FOLLOW_USER_ID, FOLLOW_SEC_UID);
            if (Objects.isNull(myFollow) || !Objects.equals(myFollow.getStatus_code(), 0L)) {
                log.warn("findAllMyFollow empty/status={}, attempt={}",
                        Objects.isNull(myFollow) ? "null" : myFollow.getStatus_code(), attempt);
                boolean authFailure = Objects.nonNull(myFollow)
                        && Objects.equals(myFollow.getStatus_code(), 8L);
                if (authFailure && allowRelogin && reLogin()) {
                    return findAllMyFollow(maxCursor, 0, false);
                }
                if (attempt < 3) {
                    return findAllMyFollow(maxCursor, attempt + 1, allowRelogin);
                }
                return result;
            }
            if (CollectionUtils.isNotEmpty(myFollow.getFollowings())) {
                result.addAll(myFollow.getFollowings());
            }
            if (myFollow.isHas_more()) {
                result.addAll(findAllMyFollow(myFollow.getMin_time(), 0, allowRelogin));
            }
        } catch (Exception e) {
            log.error("findAllMyFollow error, attempt={}", attempt, e);
            if (attempt < 3) {
                return findAllMyFollow(maxCursor, attempt + 1, allowRelogin);
            }
        }
        return result;
    }

    private void findCollects(Long maxCursor, Long collectsId) {
        findCollects(maxCursor, collectsId, 0, true);
    }

    @SneakyThrows
    private void findCollects(Long maxCursor, Long collectsId, int attempt, boolean allowRelogin) {
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;
        Collect collect;
        try {
            collect = browserClient().findCollects(cursor, collectsId);
        } catch (Exception e) {
            log.error("findCollects error, attempt={}, collectsId={}", attempt, collectsId, e);
            if (attempt < 3) {
                findCollects(maxCursor, collectsId, attempt + 1, allowRelogin);
            }
            return;
        }
        if (Objects.isNull(collect) || !Objects.equals(collect.getStatus_code(), 0)) {
            log.warn("findCollects empty/status={}, attempt={}, collectsId={}",
                    Objects.isNull(collect) ? "null" : collect.getStatus_code(), attempt, collectsId);
            boolean authFailure = Objects.nonNull(collect)
                    && Objects.equals(collect.getStatus_code(), 4);
            if (authFailure && allowRelogin && reLogin()) {
                findCollects(maxCursor, collectsId, 0, false);
                return;
            }
            if (attempt < 3) {
                findCollects(maxCursor, collectsId, attempt + 1, allowRelogin);
            }
            return;
        }
        List<AwemeList> followings = collect.getAweme_list();
        if (CollectionUtils.isEmpty(followings)) {
            return;
        }
        Set<String> lines = allLine();
        for (AwemeList follow : followings) {
            Author author = follow.getAuthor();
            cancelCollect(follow.getAweme_id());
            String s = " http://www.douyin.com/user/" + author.getSec_uid();
            if (lines.stream().noneMatch(o -> StringUtils.contains(o, author.getSec_uid()))) {
                Files.write(Paths.get(RECORD_FILE), (StringUtils.trim(author.getNickname()) + s + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.WRITE);
                job(s, 0L, false);
                System.out.println(s);
            }
        }
        if (Objects.equals(1, collect.getHas_more())) {
            findCollects(collect.getCursor(), collectsId, 0, allowRelogin);
        }
    }

    @SneakyThrows
    private void cancelCollect(String id) {
        HashMap<String, String> data = Maps.newHashMap();
        data.put("action", "0");
        data.put("aweme_type", "0");
        data.put("aweme_id", id);
        StringBuilder stringBuilder = fill(new StringBuilder("https://www.douyin.com/aweme/v1/web/aweme/collect/?"))
                .append("&aid=").append(AID)
                .append("&update_version_code=170400&pc_client_type=1&pc_libra_divert=Windows")
                .append("&update_version_code=170400&support_h265=1&support_dash=1")
                .append("&version_name=17.4.0&browser_language=zh-CN&browser_platform=Win32")
                .append("&browser_name=Edge&browser_version=135.0.0.0")
                .append("&engine_name=Blink&engine_version=135.0.0.0")
                .append("&os_version=10&cpu_core_num=4&device_memory=8&platform=PC")
                .append("&downlink=1.5&effective_type=4g&round_trip_time=100")
                .append("&webid=7462351083349313039")
                .append("&uifid=a061b0aeafc5f81960457244188fc0aa90cbddf27d4ca38da72bcf4a38905bfbd042888f45c098796e5330c5e3a1897f766debb98abfa28a5974cf4ccef6a778bfdad7274f4651d609be5daf25292f74a01caf58367aa2bce2edd880b5e4bb523dd6cc27aa147ce0e1108a2e9bf50b63c8c76f3e694d0a00e455800b9543747dfe7e32e42b47d7f2d37a52ee4f8948f0dc0a00aa3098b239243445c2450ecc19")
                .append("&msToken=24D7x8wQza7nmFU12cSc45jXP8MMoUbH32FzWG9KcfQ38hPs9fI6NgjOnDoWax8QIYLT08e7hc2Oc3xseO_UBV6NidyEosnt2Tg6rF3BS1GLSP2c5_2VHj8x6A68mOwwBYveW7MLt1_3vQy2xt22vBGezFd22PnPMNNKfqkJOosPQQ%3D%3D")
                .append("&a_bogus=D7UjDeULQZR5OVKSmCkw93IUxzolNT8yCai2bLqPSOP4TZzaE8NrZzzSaKORbJanTuMhwlI7ziCruVfOKtWxZCCkLmpvuis6Bt2VIzsL8HZmbBkg7r68e4bxoiTTUSGY8%2FIAiZRRAsMK2EOWVr9TAdI7F%2FvrRbDdMq-vV%2FzjY9Km0WSji92Ca5ydNh7qrD%3D%3D")
                .append("&verifyFp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ")
                .append("&fp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ");
        Map<String, String> header = buildHeader("https://www.douyin.com/user/self?from_tab_name=main&modal_id=" + id + "&showTab=favorite_collection");
        header.put("x-secsdk-csrf-token", "DOWNGRADE");
        Document post = HttpUtils.postHeader(stringBuilder.toString(), data, header);
        if (StringUtils.contains(post.body().text(), "3009008")) {
            cancelCollect(id);
        }
    }

    private String findShareFullUrl(String shareUrl) throws Exception {
        Document document = HttpUtils.get(shareUrl, buildHeader(shareUrl));
        String s = document.baseUri();
        if (StringUtils.contains(s, "www.douyin.com/user/")) {
            Matcher matcher = pattern.matcher(document.toString());
            if (matcher.find()) {
                String group = matcher.group();
                s = s + "&" + group + "&";
            }
        }
        return s;
    }

    public Map<String, String> buildHeader(String refererUrl) {
        HashMap<String, String> map = Maps.newHashMap();
        map.put("cookie", COOKIE);
        map.put("referer", refererUrl);
        // HttpUtils 默认会随机使用 Android/旧浏览器 UA，与桌面 cookie 会话不一致，这里显式覆盖。
        map.put("user-agent", DouyinBrowserClient.DESKTOP_UA);
        return map;
    }

    public Map<String, String> buildCookie() {
        String[] split = StringUtils.split(COOKIE, ";");
        return Arrays.stream(split).collect(Collectors.toMap(o -> StringUtils.substringBefore(o, "="), o -> StringUtils.substringAfter(o, "="), (o1, o2) -> o2));
    }

    private String removeChar(String filename) {
        return StringUtils.trim(StringUtils.replaceEach(filename, new String[]{"/", "\\", ":", "*", "?", "\"", "<", ">", "|"}, new String[]{"", "", "", "", "", "", "", "", ""}));
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

    public void follow(String uid, boolean follow) {
        String response = browserClient().follow(uid, follow);
        log.info("follow uid={}, type={}, response={}", uid, follow ? 1 : 0, response);
    }
}
