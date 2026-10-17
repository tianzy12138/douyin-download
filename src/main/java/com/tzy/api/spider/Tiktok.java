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
import java.io.IOException;
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
    private static final Pattern pattern = Pattern.compile("from\\_aid=(\\d+)");
    static final String FOLLOW_USER_ID = "95469136501";
    static final String FOLLOW_SEC_UID = "MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc";
    private static final String NEW_BASE_PATH = "F:\\douyin";
    private static final String AID = "6383";
    private static final String FALLBACK_COOKIE = System.getProperty("douyin.cookie", "enter_pc_once=1; UIFID_TEMP=ee69abbd54cdddb154b01110285f038c0f724c22a9fa6fa6ff8c47d6ab1f845cd9d8b19e25aab7404ad22aba5bd575400235ed6680c0133c7c9e5a4eb0982694c270a7862aa17e67ec959dabca51e021; hevc_supported=true; is_dash_user=1; bd_ticket_guard_client_web_domain=2; n_mh=_7yVq3g2tC6LtBV5sOfmCm8-LrMgWCU850nabGZZQcc; is_staff_user=false; has_biz_token=false; UIFID=ee69abbd54cdddb154b01110285f038c0f724c22a9fa6fa6ff8c47d6ab1f845cd9d8b19e25aab7404ad22aba5bd57540f6632a1cfb0b884bcd5318e73d167033a8ab5369f79a7234d39c248658d44d83a19e25cafc46082afcce6265444cda306429ab16b50e1fb058156fea17ba0b9fba62fc51e976db39735f3addbc38738ee10014e918e351fc04ac16f3da839c71bdf6f18fbf4575baac7a3154927c278f; my_rd=2; live_private_user=0; d_ticket=8ec0bf32b478ce7bc68ef8a066480ff92fc72; SearchResultListTypeChangedManually=%221%22; SEARCH_RESULT_LIST_TYPE=%22multi%22; live_use_vvc=%22false%22; theme=%22dark%22; manual_theme=%22dark%22; __druidClientInfo=JTdCJTIyY2xpZW50V2lkdGglMjIlM0E4NjIlMkMlMjJjbGllbnRIZWlnaHQlMjIlM0E4NTglMkMlMjJ3aWR0aCUyMiUzQTg2MiUyQyUyMmhlaWdodCUyMiUzQTg1OCUyQyUyMmRldmljZVBpeGVsUmF0aW8lMjIlM0ExJTJDJTIydXNlckFnZW50JTIyJTNBJTIyTW96aWxsYSUyRjUuMCUyMChXaW5kb3dzJTIwTlQlMjAxMC4wJTNCJTIwV2luNjQlM0IlMjB4NjQpJTIwQXBwbGVXZWJLaXQlMkY1MzcuMzYlMjAoS0hUTUwlMkMlMjBsaWtlJTIwR2Vja28pJTIwQ2hyb21lJTJGMTUxLjAuMC4wJTIwU2FmYXJpJTJGNTM3LjM2JTIwRWRnJTJGMTUxLjAuMC4wJTIyJTdE; passport_assist_user=CjyQVtbZMzDEySO--ZInbjucIb3450Aa0WSsKndI9fzEnz5OKWdfd5VWTQycMW83XC-zWK_PVKzowrrpsdUaSgo8AAAAAAAAAAAAAFDNSP-zVmcImwkiW1lvGtOdszHOdMFMJCudlCHHeHvXAEL4sPU9mga1vn_KGIgAJzr-EICImg4Yia_WVCABIgEDLWGKSw%3D%3D; uid_tt=410743a203fabf775c9a52e6adf846ca; uid_tt_ss=410743a203fabf775c9a52e6adf846ca; sid_tt=d44126642bfb285b664750a404ea87b4; sessionid=d44126642bfb285b664750a404ea87b4; sessionid_ss=d44126642bfb285b664750a404ea87b4; bd_ticket_guard_ts_sign_id=ts.2.80689ddfcb34b2a; login_time=1787237599156; _bd_ticket_crypt_cookie=0d1f4af857d4aee9c34f4a2a7d5889d8; LivePausePop=%22%257B%2522todayCount%2522%253A1%252C%2522closeNum%2522%253A0%252C%2522todayShowRoom%2522%253A%25227687073350828575515%2522%252C%2522lastTimer%2522%253A1789787243844%257D%22; passport_csrf_token=1fdb0cf54a20bbb578006b66986fd120; passport_csrf_token_default=1fdb0cf54a20bbb578006b66986fd120; JXEntranceNegative=1; __live_version__=%221.1.5.5978%22; volume_info=%7B%22isMute%22%3Afalse%2C%22isUserMute%22%3Afalse%2C%22volume%22%3A1%7D; sid_guard=d44126642bfb285b664750a404ea87b4%7C1791367109%7C5184000%7CSun%2C+06-Dec-2026+09%3A58%3A29+GMT; session_tlb_tag=sttt%7C6%7C1EEmZCv7KFtmR1CkBOqHtP_________BcsA2dq95kihJ9cH3J9YXF3sbUuamm4rcEAAQj5z5unE%3D; sid_ucp_v1=1.0.0-KDAzMzExODE2ZjYzM2VlNTc3MGVkOTBhOWQyZmVkYzUzOTVhZGJlNTEKHwj11J3T4wIQxa-Y1gYY7zEgDDCcp6zVBTgFQPsHSAQaAmxmIiBkNDQxMjY2NDJiZmIyODViNjY0NzUwYTQwNGVhODdiNA; ssid_ucp_v1=1.0.0-KDAzMzExODE2ZjYzM2VlNTc3MGVkOTBhOWQyZmVkYzUzOTVhZGJlNTEKHwj11J3T4wIQxa-Y1gYY7zEgDDCcp6zVBTgFQPsHSAQaAmxmIiBkNDQxMjY2NDJiZmIyODViNjY0NzUwYTQwNGVhODdiNA; __security_mc_1_s_sdk_crypt_sdk=e140f9b7-447a-a6b2; __security_mc_1_s_sdk_cert_key=4f210eda-4312-83dd; __security_mc_1_s_sdk_sign_data_key_web_protect=72b9f185-4b16-8f36; publish_badge_show_info=%220%2C0%2C0%2C1791678464615%22; shareRecommendGuideTagCount=4; is_dbsc=true; is_support_rtm_web_ts=1; SelfTabRedDotControl=%5B%7B%22id%22%3A%227053341385214134302%22%2C%22u%22%3A103%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227694878159191148570%22%2C%22u%22%3A4%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227648145771091232818%22%2C%22u%22%3A62%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227642410508707956787%22%2C%22u%22%3A8%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227642805861626153023%22%2C%22u%22%3A100%2C%22c%22%3A71%7D%2C%7B%22id%22%3A%227642259216715122728%22%2C%22u%22%3A84%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227431109608435025958%22%2C%22u%22%3A82%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227322413096730167296%22%2C%22u%22%3A249%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227623757272627284002%22%2C%22u%22%3A79%2C%22c%22%3A52%7D%2C%7B%22id%22%3A%227642107925468547135%22%2C%22u%22%3A36%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227658142760402978867%22%2C%22u%22%3A30%2C%22c%22%3A14%7D%2C%7B%22id%22%3A%227582901916650768435%22%2C%22u%22%3A162%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227643829184016189491%22%2C%22u%22%3A32%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227617826025392588810%22%2C%22u%22%3A52%2C%22c%22%3A0%7D%2C%7B%22id%22%3A%227631348274598774803%22%2C%22u%22%3A8%2C%22c%22%3A0%7D%5D; PhoneResumeUidCacheV1=%7B%2295469136501%22%3A%7B%22time%22%3A1792251563358%2C%22noClick%22%3A4%7D%7D; FOLLOW_NUMBER_YELLOW_POINT_INFO=%22MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc%2F1792252800000%2F0%2F1792251563643%2F0%22; playRecommendGuideTagCount=4; totalRecommendGuideTagCount=6; x_tt_token=00d44126642bfb285b664750a404ea87b402eb8de4f5f2ee843f4476215c90b5ad179ea60234dbc2423817aac8bd36194715280018a693a6061671cad34343d99ac939c92b4323da10896f2ca219d99f864872eaea847ec1ff1047af941bbd8d3ab0e--0a490a204f9cfb4858edb7e26d58016e71e9321e7b7bb2fbf51f0de79e1b909e0e6086001220c140277a4e961332c5b66a55aaa355a141cbb2d4c6016566b0a77d81e8163e4718f6b4d309-3.0.4; gulu_source_res=eyJwX2luIjoiNzQ3Y2NkNjA3NTljYTg2MTdkZTkzMzU0MTYwNGVkYTc0ZmFkNDE1MGFiNTI2ZTBkYmZkMDA4ZDUxZWE5NzUzZCJ9; stream_recommend_feed_params=%22%7B%5C%22cookie_enabled%5C%22%3Atrue%2C%5C%22screen_width%5C%22%3A1920%2C%5C%22screen_height%5C%22%3A1080%2C%5C%22browser_online%5C%22%3Atrue%2C%5C%22cpu_core_num%5C%22%3A12%2C%5C%22device_memory%5C%22%3A32%2C%5C%22downlink%5C%22%3A1.6%2C%5C%22effective_type%5C%22%3A%5C%224g%5C%22%2C%5C%22round_trip_time%5C%22%3A0%7D%22; strategyABtestKey=%221792253188.128%22; FOLLOW_LIVE_POINT_INFO=%22MS4wLjABAAAApuob9iqYh2t0x6e8RKSYd_aFQlJrPmPSy7mYw8QdFuc%2F1792339200000%2F0%2F1792253188594%2F0%22; ttwid=1%7CLUMn2KjZsSGIXyjkevud0RsldBWhytL43pmqngktF6Q%7C1792253189%7Cef283f2e0a3ecfb1c2a29334ef7ab1315711c3a6d9620f0ac7bd73e94862a580; home_can_add_dy_2_desktop=%221%22; bd_ticket_guard_client_data=eyJiZC10aWNrZXQtZ3VhcmQtdmVyc2lvbiI6MiwiYmQtdGlja2V0LWd1YXJkLWl0ZXJhdGlvbi12ZXJzaW9uIjoxLCJiZC10aWNrZXQtZ3VhcmQtcmVlLXB1YmxpYy1rZXkiOiJCS3BwSnYzZTQ2eWI3SHZhR0tidTdoS3JzbkFpQzMrNjd5dHA4Wk92bHlKaEpSb0J2WFdzRCtHQzQrd1hlbHdlYThKU1Q5MnV5WURPTGVnM0tCcTVveDA9IiwiYmQtdGlja2V0LWd1YXJkLXdlYi12ZXJzaW9uIjoyfQ%3D%3D; biz_trace_id=c1c0ef76; odin_tt=261716089a5c6831af7651e26cc86451345e006bdb3a00546b69eb858250e85b6ab517a03898e950a20a88f06db63a4f20efd83b17a4d10ec1ee63a50c6f26cebf3e832e0dabfe3c61fbfec8936edde0; sdk_source_info=7e276470716a68645a606960273f276364697660272927676c715a6d6069756077273f276364697660272927666d776a68605a607d71606b766c6a6b5a7666776c7571273f275e58272927666a6b766a69605a696c6061273f27636469766027292762696a6764695a7364776c6467696076273f275e582729277672715a646971273f2763646976602729277f6b5a666475273f2763646976602729276d6a6e5a6b6a716c273f2763646976602729276c6b6f5a7f6367273f27636469766027292771273f273d32303d3c34363037373c3234272927676c715a75776a716a666a69273f2763646976602778; bit_env=T-XJkazdAsS9mqNQQL6Co47XsFh2wvIgqYFppQP_sqimMnZ4hYjBVi9qGinSj1wVtEDYw-5fmFMxEmQs1QNXokrkW-hASfA9jvMLLGbj3og_1e5ECJ9se3EV-LJEdFhuVQzdbih3Z4cGtTgr_TkpViq19PL_rY31ubdMSIe-db2XsN33XICDbJpTe_N3dXkyQCEZp40st0PAqxFTsxydytgVxEd_140iPjgsbNAJXxyVGsmTKAOL-GqZ2_LFdK1c8cfWYIMdMhJ3Yl94ZI-ov9a66d6ojm56pm94YHKU5QYQiiHyn1bxYOMFxvAaHhOmcokkkwj5i7LG7T9rICEBE1iCQwfxlynfPF0aYajUzdrDzC-OcEJHOiWKJkQD2psS37z7rHxq85iCVAQaFaMzSD9Ds5sxZmQ52QwDYqafhlk3LzlBePmMdKSp6vcdp9aOXZ8Xfh1cY52x0aZ5KbyqqDVbehkya5N4qdhZAz8mgwg4ubpyJ5D-OCKQmRlpNtq__f-xska4fzP9HpZ4UbW7TOukz5fUa24SGO4Y_WV5VPQ%3D; passport_auth_mix_state=aezsuvcw428xujf5427yku22myvigtse; IsDouyinActive=true; stream_player_status_params=%22%7B%5C%22is_auto_play%5C%22%3A0%2C%5C%22is_full_screen%5C%22%3A0%2C%5C%22is_full_webscreen%5C%22%3A1%2C%5C%22is_mute%5C%22%3A0%2C%5C%22is_speed%5C%22%3A1%2C%5C%22is_visible%5C%22%3A1%7D%22");
    /**
     * 优先级：-Ddouyin.cookie > cookies.txt > 内置旧 cookie。
     */
    static String COOKIE = resolveCookie();
    private static final String ILLEGAL_CHARACTERS_REGEX = "[\\/:*?\"<>|\\n\\.]";
    private static final String BASE_PATH = "E:\\douyin";

    protected ExecutorService threadPool;

    private DouyinBrowserClient browserClient;
    private DouyinBrowserClient loginBrowserClient;

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
//        tiktok.findCollects(0L, 7492594126609848114L);
//        tiktok.move(null);
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
        ArrayList<String> filePaths = Lists.newArrayList(RECORD_FILE);
        Set<String> result = Sets.newHashSet();
        for (String filePath : filePaths) {
            result.addAll(Files.readAllLines(Paths.get(filePath)));
        }
        return result;
    }

    public void close() {
        threadPool.shutdown();
        synchronized (this) {
            if (Objects.nonNull(browserClient)) {
                browserClient.close();
                browserClient = null;
            }
            if (Objects.nonNull(loginBrowserClient)) {
                loginBrowserClient.close();
                loginBrowserClient = null;
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
                if (attempt < 3) {
                    findContentWithHandle(shareUrl, maxCursor, handle, update, attempt + 1);
                }
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
        String finalAid = StringUtils.isBlank(aid) ? AID : aid;
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonRootBean bean = browserClient().findPosts(cursor, secUserId, finalAid);
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
        if (Objects.isNull(browserClient)) {
            // 公开作品接口：丢弃旧 ttwid，避免被风控的 ttwid 导致空响应
            browserClient = new DouyinBrowserClient(COOKIE, true);
        }
        return browserClient;
    }

    private synchronized DouyinBrowserClient loginBrowserClient() {
        if (Objects.isNull(loginBrowserClient)) {
            // 关注/收藏接口：保留 cookie 中的 ttwid，服务端才会识别登录态
            loginBrowserClient = new DouyinBrowserClient(COOKIE, false);
        }
        return loginBrowserClient;
    }

    private ArrayList<Followings> findAllMyFollow(Long maxCursor) {
        return findAllMyFollow(maxCursor, 0);
    }

    private ArrayList<Followings> findAllMyFollow(Long maxCursor, int attempt) {
        ArrayList<Followings> result = Lists.newArrayList();
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;
        try {
            MyFollow myFollow = loginBrowserClient().findMyFollow(cursor, FOLLOW_USER_ID, FOLLOW_SEC_UID);
            if (Objects.isNull(myFollow) || !Objects.equals(myFollow.getStatus_code(), 0L)) {
                log.warn("findAllMyFollow empty/status={}, attempt={}",
                        Objects.isNull(myFollow) ? "null" : myFollow.getStatus_code(), attempt);
                if (Objects.nonNull(myFollow) && Objects.equals(myFollow.getStatus_code(), 8L)) {
                    log.warn("关注列表 status=8：登录态无效或被风控，请在浏览器重新登录后用 "
                            + "-Ddouyin.cookie=... 传入新的完整 cookie");
                }
                if (attempt < 3) {
                    return findAllMyFollow(maxCursor, attempt + 1);
                }
                return result;
            }
            if (CollectionUtils.isNotEmpty(myFollow.getFollowings())) {
                result.addAll(myFollow.getFollowings());
            }
            if (myFollow.isHas_more()) {
                result.addAll(findAllMyFollow(myFollow.getMin_time(), 0));
            }
        } catch (Exception e) {
            log.error("findAllMyFollow error, attempt={}", attempt, e);
            if (attempt < 3) {
                return findAllMyFollow(maxCursor, attempt + 1);
            }
        }
        return result;
    }

    private void findCollects(Long maxCursor, Long collectsId) {
        findCollects(maxCursor, collectsId, 0);
    }

    @SneakyThrows
    private void findCollects(Long maxCursor, Long collectsId, int attempt) {
        long cursor = Objects.isNull(maxCursor) ? 0L : maxCursor;
        Collect collect;
        try {
            collect = loginBrowserClient().findCollects(cursor, collectsId);
        } catch (Exception e) {
            log.error("findCollects error, attempt={}, collectsId={}", attempt, collectsId, e);
            if (attempt < 3) {
                findCollects(maxCursor, collectsId, attempt + 1);
            }
            return;
        }
        if (Objects.isNull(collect) || !Objects.equals(collect.getStatus_code(), 0)) {
            log.warn("findCollects empty/status={}, attempt={}, collectsId={}",
                    Objects.isNull(collect) ? "null" : collect.getStatus_code(), attempt, collectsId);
            if (Objects.nonNull(collect) && Objects.equals(collect.getStatus_code(), 4)) {
                log.warn("收藏列表 status=4：登录态无效或 collects_id 失效，请更新 COOKIE 和 collects_id");
            }
            if (attempt < 3) {
                findCollects(maxCursor, collectsId, attempt + 1);
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
            findCollects(collect.getCursor(), collectsId, 0);
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
