package com.tzy.api.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tzy.api.common.HttpUtils;
import com.tzy.api.common.ThreadPoolUtils;
import com.tzy.api.entity.User;
import com.tzy.api.spider.dto.*;
import jakarta.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DownloadService {

    private final DataRecordService dataRecordService;
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

    public DownloadService(DataRecordService dataRecordService) {
        this.threadPool = ThreadPoolUtils.getThreadPool(5, DownloadService.class.getSimpleName());
        this.dataRecordService = dataRecordService;
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

    @SneakyThrows
    public void cancelCollect(String id) {
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

    private void doSave(AwemeList o) {
        dataRecordService.saveFromAwemeList(o);
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

    public void downloadUserContent(User user, boolean update) {
        int attempts = 0;
        while (attempts < maxAttempts) {
            try {
//                findContentWithHandle(user.getShareUrl(), 0L, this::doDownload, update);
                findContentWithHandle(user.getShareUrl(), 0L);
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

    public void findContentWithHandle(String shareUrl, Long maxCursor) {
        log.info("正在处理用户数据：{}", shareUrl);
        try {
            JsonRootBean jsonRootBean = findPostsByShareUrl(shareUrl, maxCursor);
            if (Objects.isNull(jsonRootBean)) {
                log.warn("RESPONSE_IS_NULL: {} ,{}", maxCursor, shareUrl);
                findContentWithHandle(shareUrl, maxCursor);
                return;
            }
            Long status_code = jsonRootBean.getStatus_code();
            Long has_more = jsonRootBean.getHas_more();
            if (status_code != 0) {
                log.warn("STATUS_CODE_IS_NOT_ZERO: {} ,{}", maxCursor, shareUrl);
                findContentWithHandle(shareUrl, maxCursor);
                return;
            }
            List<AwemeList> list = jsonRootBean.getAweme_list();
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            int total = list.size();
            int current = list.size();
            for (AwemeList awemeList : list) {
//                doDownload(awemeList);
                doSave(awemeList);
            }
            if (has_more == 1 && total == current) {
                findContentWithHandle(shareUrl, jsonRootBean.getMax_cursor());
            }
        } catch (Exception e) {
            log.error("findContentWithHandle error", e);
            findContentWithHandle(shareUrl, maxCursor);
        }
    }

    @SneakyThrows
    public List<AwemeList> discoverFromCollects(Long collectsId) {
        if (Objects.isNull(collectsId)) {
            collectsId = 7492594126609848114L;
        }
        List<AwemeList> newAuthors = Lists.newArrayList();
        discoverFromCollects(0L, collectsId, newAuthors);
        return newAuthors;
    }

    private void discoverFromCollects(Long maxCursor, Long collectsId, List<AwemeList> newAuthors) {
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
            newAuthors.add(follow);
        }
        if (Objects.equals(1, collect.getHas_more())) {
            discoverFromCollects(collect.getCursor(), collectsId, newAuthors);
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
}
