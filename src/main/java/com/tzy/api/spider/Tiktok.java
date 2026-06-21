package com.tzy.api.spider;

import com.alibaba.fastjson.JSON;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Tiktok {
    public static final String RECORD_FILE = "E:\\1.txt";
    private static final Pattern pattern = Pattern.compile("from\\_aid=(\\d+)");
    private static final String COOKIE = "enter_pc_once=1; UIFID_TEMP=ee69abbd54cdddb154b01110285f038c0f724c22a9fa6fa6ff8c47d6ab1f845cd9d8b19e25aab7404ad22aba5bd575400235ed6680c0133c7c9e5a4eb0982694c270a7862aa17e67ec959dabca51e021; s_v_web_id=verify_mpiesyqx_5DoPxhsl_al7I_4skp_BuIk_ptxwAaGpCUKL; hevc_supported=true; dy_swidth=1920; dy_sheight=1080; fpk1=U2FsdGVkX1+y3kW/pKfY5AYk+MCub76whv29bR7BoWcQtKhfVcvqOBxIfftwl+J+JI+t4MMtVYpHIlx6Yq7cTw==; fpk2=d782f8d891caba24ff6aeba315180cac; is_dash_user=1; passport_csrf_token=d1b21f1849e0a2bbceae7b126f93f856; passport_csrf_token_default=d1b21f1849e0a2bbceae7b126f93f856; bd_ticket_guard_client_web_domain=2; n_mh=_7yVq3g2tC6LtBV5sOfmCm8-LrMgWCU850nabGZZQcc; is_staff_user=false; has_biz_token=false; __security_server_data_status=1; UIFID=ee69abbd54cdddb154b01110285f038c0f724c22a9fa6fa6ff8c47d6ab1f845cd9d8b19e25aab7404ad22aba5bd57540f6632a1cfb0b884bcd5318e73d167033a8ab5369f79a7234d39c248658d44d83a19e25cafc46082afcce6265444cda306429ab16b50e1fb058156fea17ba0b9fba62fc51e976db39735f3addbc38738ee10014e918e351fc04ac16f3da839c71bdf6f18fbf4575baac7a3154927c278f; my_rd=2; live_private_user=0; d_ticket=8ec0bf32b478ce7bc68ef8a066480ff92fc72; passport_mfa_token=CjWqwLccB9gI0M7TrX6C0%2FydB5fncKeRhPFu0wQIBZieG9yH1O98w5Uh10T6p9fRsH4btN52zxpKCjwAAAAAAAAAAAAAUHt%2Ffcepry3TCtHgi45111KKmzfSNVn9imsJBAdHpJEomHIG1N25tQevz5rlNL6zfZMQz%2BiSDhj2sdFsIAIiAQM5bpVv; SearchResultListTypeChangedManually=%221%22; SEARCH_RESULT_LIST_TYPE=%22multi%22; live_use_vvc=%22false%22; download_guide=%220%2F%2F1%22; FOLLOW_RED_POINT_INFO=%221%22; DiscoverFeedExposedAd=%7B%7D; publish_badge_show_info=%220%2C0%2C0%2C1781399259410%22; __security_mc_1_s_sdk_crypt_sdk=0f3073fc-4c7a-b4d1; __security_mc_1_s_sdk_cert_key=a54e63da-43d2-ba21; FRIEND_NUMBER_RED_POINT_INFO=%22MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc%2F1781625600000%2F1781618623603%2F0%2F0%22; __live_version__=%221.1.5.2904%22; PhoneResumeUidCacheV1=%7B%2295469136501%22%3A%7B%22time%22%3A1781625313931%2C...";
    private static final String NEW_BASE_PATH = "G:\\douyin";
    private static final String AID = "6383";
    private static final String ILLEGAL_CHARACTERS_REGEX = "[\\/:*?\"<>|\\n\\.]";
    private static final String BASE_PATH = "E:\\douyin";

    protected ExecutorService threadPool;

    public Tiktok() {
        this.threadPool = ThreadPoolUtils.getThreadPool(5, Tiktok.class.getSimpleName());
    }

    public static String removeIllegalCharacters(String filePath) {
        if (filePath == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(ILLEGAL_CHARACTERS_REGEX);
        Matcher matcher = pattern.matcher(filePath);
        return matcher.replaceAll("");
    }

    public static void main(String[] args) throws Exception {
        Tiktok tiktok = new Tiktok();
        tiktok.findCollects(0L, 7492594126609848114L);
        tiktok.jobList(true);
        tiktok.close();
    }

    @SneakyThrows
    private void confirm() {
        ArrayList<String> filePaths = Lists.newArrayList("F:\\unknown.txt");
        Set<String> strings = Sets.newHashSet();
        for (String filePath : filePaths) {
            strings.addAll(Files.readAllLines(Paths.get(filePath)));
        }
        for (String string : strings) {
            String http = StringUtils.substringAfter(string, "http");
            Profile profile = this.findProfile("http" + http);
            boolean error = true;
            Integer aweme_count = 0;
            if (Objects.isNull(profile)) {
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
                Files.write(Paths.get("F:\\2.txt"), (string + "\r\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
        }
    }

    @SneakyThrows
    private void delete(DirectoryStream<Path> basePath) {
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
    private void cancelFollow() {
        Set<String> strings = allLine();
        ArrayList<Followings> allMyFollow = findAllMyFollow(0L);
        Set<Followings> collect = allMyFollow.stream()
                .filter(o -> strings.stream().anyMatch(j -> StringUtils.contains(j, o.getSec_uid())))
                .collect(Collectors.toSet());
        for (Followings s : collect) {
            follow(s.getUid(), false);
        }
    }

    @SneakyThrows
    private Profile findProfile(String secUserId) {
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
                long threeMonth = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli();
                long l = path.toFile().lastModified();
                if (l > threeMonth) {
                    continue;
                }
                System.out.println(path);
                String destPath = StringUtils.remove(path.toString(), BASE_PATH);
                destPath = NEW_BASE_PATH + StringUtils.remove(destPath, path.getFileName().toString());
                System.out.println(destPath);
                Path toPath = Paths.get(destPath);
                createDir(toPath);
                toPath = Paths.get(toPath.toString(), path.getFileName().toString());
                if (!Files.exists(toPath)) {
                    Files.move(path, toPath, StandardCopyOption.REPLACE_EXISTING);
                }
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
        ArrayList<String> filePaths = Lists.newArrayList(RECORD_FILE);
        Set<String> result = Sets.newHashSet();
        for (String filePath : filePaths) {
            result.addAll(Files.readAllLines(Paths.get(filePath)));
        }
        return result;
    }

    public void close() {
        threadPool.shutdown();
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
        List<AwemeList> result = Lists.newArrayList();
        JsonRootBean jsonRootBean = null;
        try {
            jsonRootBean = findPostsByShareUrl(shareUrl, maxCursor);
        } catch (Exception e) {
            log.error("findPostsByShareUrl error", e);
            findAllContent(shareUrl, maxCursor);
        }
        if (Objects.isNull(jsonRootBean)) {
            log.warn("RESPONSE_IS_NULL:{},{}", maxCursor, shareUrl);
            return result;
        }
        Long status_code = jsonRootBean.getStatus_code();
        Long has_more = jsonRootBean.getHas_more();
        if (status_code != 0) {
            result.addAll(findAllContent(shareUrl, maxCursor));
            return result;
        }
        result.addAll(jsonRootBean.getAweme_list());
        if (has_more == 1) {
            result.addAll(findAllContent(shareUrl, jsonRootBean.getMax_cursor()));
        }
        return result;
    }

    public void findContentWithHandle(String shareUrl, Long maxCursor, Consumer<AwemeList> handle, boolean update) {
        log.info("正在处理登记数据：{}", shareUrl);
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
        List<AwemeList> result = Lists.newArrayList();
        JsonRootBean jsonRootBean = findPosts(maxCursor, secUid, AID);
        if (Objects.isNull(jsonRootBean)) {
            result.addAll(findAllContent(maxCursor, secUid));
            return result;
        }
        Long status_code = jsonRootBean.getStatus_code();
        Long has_more = jsonRootBean.getHas_more();
        if (status_code != 0) {
            result.addAll(findAllContent(maxCursor, secUid));
            return result;
        }
        result.addAll(jsonRootBean.getAweme_list());
        if (has_more == 1) {
            result.addAll(findAllContent(jsonRootBean.getMax_cursor(), secUid));
            return result;
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

    @SneakyThrows
    public JsonRootBean findPosts(Long maxCursor, String secUserId, String aid) {
        if (StringUtils.isBlank(secUserId) || StringUtils.isBlank(aid)) {
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
            return findPosts(maxCursor, secUserId, aid);
        }
    }

    private ArrayList<Followings> findAllMyFollow(Long maxCursor) {
        ArrayList<Followings> result = Lists.newArrayList();
        StringBuilder stringBuilder = fill(new StringBuilder("https://www.douyin.com/aweme/v1/web/user/following/list/?"));
        stringBuilder.append("&aid=" + AID);
        stringBuilder.append("&user_id=95469136501");
        stringBuilder.append("&sec_user_id=MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc");
        stringBuilder.append("&pc_client_type=1");
        stringBuilder.append("&is_top=1");
        stringBuilder.append("&address_book_access=0");
        stringBuilder.append("&gps_access=0");
        stringBuilder.append("&source_type=1");
        stringBuilder.append("&update_version_code=170400");
        stringBuilder.append("&version_code=170400");
        stringBuilder.append("&version_name=17.4.0");
        stringBuilder.append("&count=20");
        stringBuilder.append("&max_time=").append(maxCursor);
        Document document = HttpUtils.get(stringBuilder.toString(), buildHeader("https://www.douyin.com/user/self?from_tab_name=main&showTab=post"));
        MyFollow myFollow = JSON.parseObject(document.body().text(), MyFollow.class);
        List<Followings> followings = myFollow.getFollowings();
        if (CollectionUtils.isNotEmpty(followings)) {
            result.addAll(followings);
        }
        if (myFollow.isHas_more()) {
            result.addAll(findAllMyFollow(myFollow.getMin_time()));
        }
        return result;
    }

    @SneakyThrows
    private void findCollects(Long maxCursor, Long collectsId) {
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
        stringBuilder.append("&webid=7462351083349313039");
        stringBuilder.append("&uifid=a061b0aeafc5f81960457244188fc0aa90cbddf27d4ca38da72bcf4a38905bfbd042888f45c098796e5330c5e3a1897f766debb98abfa28a5974cf4ccef6a778bfdad7274f4651d609be5daf25292f74a01caf58367aa2bce2edd880b5e4bb523dd6cc27aa147ce0e1108a2e9bf50b63c8c76f3e694d0a00e455800b9543747dfe7e32e42b47d7f2d37a52ee4f8948f0dc0a00aa3098b239243445c2450ecc19");
        stringBuilder.append("&msToken=24D7x8wQza7nmFU12cSc45jXP8MMoUbH32FzWG9KcfQ38hPs9fI6NgjOnDoWax8QIYLT08e7hc2Oc3xseO_UBV6NidyEosnt2Tg6rF3BS1GLSP2c5_2VHj8x6A68mOwwBYveW7MLt1_3vQy2xt22vBGezFd22PnPMNNKfqkJOosPQQ");
        stringBuilder.append("&a_bogus=Qj0fkHSjmdWnPV%2FG8Okh9-cl%2FFgArBWygMixbr-THNu9GwecoYPnZPczaxumZSFj7bMkwIqHuEt%2FPDdcKGUwZC9kwmhfSYGS1tVnI0mogqqvTUz%2FDNjpCwWFqwMn0RGqlA57iI4I8UJH6fxAhrdD%2Fply9KLC558BPpxWk2uci9Bh1FgAL3nrPpGdYwTKUI9W");
        stringBuilder.append("&verifyFp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ");
        stringBuilder.append("&fp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ");

        Document document = HttpUtils.get(stringBuilder.toString(), buildHeader("https://www.douyin.com/user/self?from_tab_name=main&showSubTab=favorite_folder&showTab=favorite_collection"));
        Collect collect = JSON.parseObject(document.body().text(), Collect.class);
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
            findCollects(collect.getCursor(), collectsId);
        }
    }

    @SneakyThrows
    private void cancelCollect(String id) {
        HashMap<String, String> data = Maps.newHashMap();
        data.put("action", "0");
        data.put("aweme_type", "0");
        data.put("aweme_id", id);
        StringBuilder stringBuilder = fill(new StringBuilder("https://www.douyin.com/aweme/v1/web/aweme/collect/?"));
        stringBuilder.append("&aid=" + AID);
        stringBuilder.append("&update_version_code=170400");
        stringBuilder.append("&pc_client_type=1");
        stringBuilder.append("&pc_libra_divert=Windows");
        stringBuilder.append("&update_version_code=170400");
        stringBuilder.append("&support_h265=1");
        stringBuilder.append("&support_dash=1");
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
        stringBuilder.append("&downlink=1.5");
        stringBuilder.append("&effective_type=4g");
        stringBuilder.append("&round_trip_time=100");
        stringBuilder.append("&webid=7462351083349313039");
        stringBuilder.append("&uifid=a061b0aeafc5f81960457244188fc0aa90cbddf27d4ca38da72bcf4a38905bfbd042888f45c098796e5330c5e3a1897f766debb98abfa28a5974cf4ccef6a778bfdad7274f4651d609be5daf25292f74a01caf58367aa2bce2edd880b5e4bb523dd6cc27aa147ce0e1108a2e9bf50b63c8c76f3e694d0a00e455800b9543747dfe7e32e42b47d7f2d37a52ee4f8948f0dc0a00aa3098b239243445c2450ecc19");
        stringBuilder.append("&msToken=24D7x8wQza7nmFU12cSc45jXP8MMoUbH32FzWG9KcfQ38hPs9fI6NgjOnDoWax8QIYLT08e7hc2Oc3xseO_UBV6NidyEosnt2Tg6rF3BS1GLSP2c5_2VHj8x6A68mOwwBYveW7MLt1_3vQy2xt22vBGezFd22PnPMNNKfqkJOosPQQ%3D%3D");
        stringBuilder.append("&a_bogus=D7UjDeULQZR5OVKSmCkw93IUxzolNT8yCai2bLqPSOP4TZzaE8NrZzzSaKORbJanTuMhwlI7ziCruVfOKtWxZCCkLmpvuis6Bt2VIzsL8HZmbBkg7r68e4bxoiTTUSGY8%2FIAiZRRAsMK2EOWVr9TAdI7F%2FvrRbDdMq-vV%2FzjY9Km0WSji92Ca5ydNh7qrD%3D%3D");
        stringBuilder.append("&verifyFp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ");
        stringBuilder.append("&fp=verify_m8ky9qta_okERLfGL_yBIW_4m0M_8Ex4_la2xOXGh4ZeQ");
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
        String body = "type=" + (follow ? "1" : "0") + "&user_id=" + uid;
        StringBuilder stringBuilder = new StringBuilder("https://www.douyin.com/aweme/v1/web/commit/follow/user/?");
        stringBuilder.append("device_platform=webapp&aid=6383&channel=channel_pc_web&pc_client_type=1&pc_libra_divert=Windows&update_version_code=170400&support_h265=1&support_dash=1&version_code=170400&version_name=17.4.0&cookie_enabled=true&screen_width=1920&screen_height=1080&browser_language=zh-CN&browser_platform=Win32&browser_name=Edge&browser_version=142.0.0.0&browser_online=true&engine_name=Blink&engine_version=142.0.0.0&os_name=Windows&os_version=10&cpu_core_num=4&device_memory=8&platform=PC&downlink=1.7&effective_type=4g&round_trip_time=50&webid=7462351083349313039&uifid=a061b0aeafc5f81960457244188fc0aa90cbddf27d4ca38da72bcf4a38905bfbd042888f45c098796e5330c5e3a1897f766debb98abfa28a5974cf4ccef6a778bfdad7274f4651d609be5daf25292f74a01caf58367aa2bce2edd880b5e4bb523dd6cc27aa147ce0e1108a2e9bf50b63c8c76f3e694d0a00e455800b9543747dfe7e32e42b47d7f2d37a52ee4f8948f0dc0a00aa3098b239243445c2450ecc19&msToken=_ILCWC7rjPtD90iZ9srviv06CVJNZzs046Kwvr_BTdPTeOtfNiQaynux47smLPKKJcdk8UnvbFbWMnFVIu3uaKA-UXjMnnOMyiJPDpxNIlswtii0tdyrytJ8nLquqLx9LQRvRZl4J8OBulTnT0f81huJMgHkTeJqeWmBiG9Wh-hVUQ%3D%3D&a_bogus=EyUnkHyJdZRnOdMtmOsdyfPleydArBSy0aidRqBT7NK2OwUOBuNsPeNSjOYPflOnESMzwA27%2FV-WTVVOTtlbg9HpLmhvu2hR2zVcIgfoZqidG0wsErjNCzTzzwMr0csqa554iIR6MUrHgndAwHdg%2FBlySKoK5buBB3x6kMzbO9sgZ0LAD3c3PQbgE7TqMf%3D%3D&verifyFp=verify_mfqvi17k_bVO87otJ_PLtH_4Wpb_9yj7_nGF9zCYtRUCq&fp=verify_mfqvi17k_bVO87otJ_PLtH_4Wpb_9yj7_nGF9zCYtRUCq");
        Map<String, String> header = buildHeader("https://www.douyin.com/user/MS4wLjABAAAAa2PDU85smt29KVScXW9-0ZTqqbVyPiQtx3zTgY_WLRE");
        header.put("accept", "application/json, text/plain, */*");
        header.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
        header.put("bd-ticket-guard-client-data", "eyJ0c19zaWduIjoidHMuMi5mYzYyZjUyN2FiM2QyZWY0Y2Q5ODNkYzg0ZDZjNTZiNzFkZTI5ZWVmNWZiNGViNDBkNmJmZGE2YTExNjA4ZjExYzRmYmU4N2QyMzE5Y2YwNTMxODYyNGNlZGExNDkxMWNhNDA2ZGVkYmViZWRkYjJlMzBmY2U4ZDRmYTAyNTc1ZCIsInJlcV9jb250ZW50IjoidGlja2V0LHBhdGgsdGltZXN0YW1wIiwicmVxX3NpZ24iOiJSL09ReHhvT2VBeURsSnJJQlhaNFM4eUJ4eWowc2dTNXo3WFVLWjAyYUF3PSIsInRpbWVzdGFtcCI6MTc2MjY5NjAzOX0=");
        header.put("bd-ticket-guard-iteration-version", "1");
        header.put("bd-ticket-guard-ree-public-key", "BPZkL4QKWsH8DobitX5P9mGVPcSyUqP/clSka3RBgYuWTlH1maq3OkSnynHHasG2zhvi2J1lW3vnDe4KNqaMnX0=");
        header.put("bd-ticket-guard-version", "2");
        header.put("bd-ticket-guard-web-sign-type", "1");
        header.put("bd-ticket-guard-web-version", "2");
        header.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
        header.put("origin", "https://www.douyin.com");
        header.put("pragma", "no-cache");
        header.put("priority", "u=1, i");
        header.put("sec-ch-ua", "\"Microsoft Edge\";v=\"141\", \"Not?A_Brand\";v=\"8\", \"Chromium\";v=\"141\"");
        header.put("sec-ch-ua-mobile", "?0");
        header.put("sec-ch-ua-platform", "\"Windows\"");
        header.put("sec-fetch-dest", "empty");
        header.put("sec-fetch-mode", "cors");
        header.put("sec-fetch-site", "same-origin");
        header.put("uifid", "a061b0aeafc5f81960457244188fc0aa90cbddf27d4ca38da72bcf4a38905bfbd042888f45c098796e5330c5e3a1897f766debb98abfa28a5974cf4ccef6a778bfdad7274f4651d609be5daf25292f74a01caf58367aa2bce2edd880b5e4bb523dd6cc27aa147ce0e1108a2e9bf50b63c8c76f3e694d0a00e455800b9543747dfe7e32e42b47d7f2d37a52ee4f8948f0dc0a00aa3098b239243445c2450ecc19");
        header.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0");
        header.put("x-secsdk-csrf-token", "DOWNGRADE");
        header.put("x-tt-session-dtrait", "d0_29G8V5ItGD5563CvweY9PXWZYrtz0HO4WHN7gVVsF3F7btC+ndlBAu4uGiaWINWGNV/TJPyXnHWgO+jXbZjcMWG1ZOUdvAZcTk+U1iS97QJT6jTR8HzzNDqpYHeHcxNiSdr3HTRjMIJOApZfyDQzUP5K3+BOWHWM+JvO6eRZ6lQhgfbDcL8Xqd/cvObRzwSfu6GoFN3PC8dLfUkc8sl/0ySvk1kQZhKfDexE+3ZNQ9Dlsd2QkkuMiTe/JDSh2bKoFNkzQzgK1yJpdMZ2JF4k8OfXIKr55m1sSx2753vGrysdE+n5PivKXQh16bcoZnKQCEJtOkmy5BssYQB4eLLHIA==_d4HmuYgTdncdY4RTaacm1Epdo+bQKBwpHxvV4gRV8hMCGX/Nune7xxnFkSvdOHGNYaWlCM5cWMgawGvTEuZDAnlzBdBxHZem9FMK88kGzde+GrJkApOqPDBrddFq75JDwAH4AHAKYglsL1wlGK4ojGzkJ7Y3spAywIVSmUGhZWGnHjxte1cD+ab3RXIyKcOl1wv6DUdOJLhWn1OnjzG0EHX8qufv7Yiw8SMEf1gAB3NFEnVaPE/jbY7hQcg6I58ymmMvE8t4zDFAmTpcAp8byeDs1CyqfBrK3w7yrLvEGTfSOjIO1iiC/pzRdaL7vns1wT+gOY6P4IP6r9yEpsoRfkewrV3/2oQfPacnc+tM7XcjVR8Y1dJmzelvEV2/P3zYbWCJfnn3oZYnc5IHuZJGnD1GKKcG4B5LXZY+YwQk+TbmSnAMQIHhZeoYCVgcd/NisdB5sj/LmkHT0qVUBhUeqA==");
        Document post = HttpUtils.post(stringBuilder.toString(), body, header);
        System.out.println(post.body());
    }
}
