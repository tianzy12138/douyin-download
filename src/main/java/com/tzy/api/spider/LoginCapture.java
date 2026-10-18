package com.tzy.api.spider;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打开有头浏览器让用户手动登录，登录后导出 Cookie，并记录 ttwid 的签发来源。
 *
 * <p>命令行用法：
 * <pre>
 * mvn -f D:/work/code/douyin-download/pom.xml exec:java \
 *     -Dexec.mainClass=com.tzy.api.spider.LoginCapture
 * </pre>
 *
 * <p>{@link Tiktok} 在登录接口返回 status=8/4 时也会调用 {@link #loginAndExport}，
 * 登录成功后继续原来的下载流程。
 */
public final class LoginCapture {

    private static final String HOME = "https://www.douyin.com/";
    private static final long LOGIN_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final String FOLLOWING_QUERY =
            "device_platform=webapp&aid=6383&channel=channel_pc_web"
                    + "&user_id=" + Tiktok.FOLLOW_USER_ID
                    + "&sec_user_id=" + Tiktok.FOLLOW_SEC_UID
                    + "&count=1&max_time=0&pc_client_type=1"
                    + "&version_code=170400&version_name=17.4.0"
                    + "&cookie_enabled=true&platform=PC";

    private LoginCapture() {
    }

    public static void main(String[] args) {
        loginAndExport(workDir(), 60_000L);
    }

    static Path workDir() {
        return Path.of(System.getProperty("douyin.work.dir",
                System.getProperty("user.dir"))).toAbsolutePath();
    }

    /**
     * 打开有头浏览器等待登录，接口验证通过后导出 cookies.txt。
     *
     * @param keepOpenMs 导出后保持窗口打开的毫秒数，0 表示立即关闭
     * @return 登录态是否通过接口验证并成功导出
     */
    static boolean loginAndExport(Path projectDir, long keepOpenMs) {
        Path profileDir = projectDir.resolve(".browser-profile");
        Path cookieFile = projectDir.resolve("cookies.txt");
        Path reportFile = projectDir.resolve("login-report.txt");
        List<String> report = new ArrayList<>();
        boolean exported = false;

        try {
            Files.createDirectories(profileDir);
            try (Playwright playwright = Playwright.create()) {
                BrowserType.LaunchPersistentContextOptions options =
                        new BrowserType.LaunchPersistentContextOptions()
                                .setHeadless(false)
                                .setLocale("zh-CN")
                                .setViewportSize(1440, 900)
                                .setArgs(List.of("--disable-blink-features=AutomationControlled"));

                BrowserContext context;
                try {
                    context = playwright.chromium().launchPersistentContext(profileDir,
                            options.setChannel("msedge"));
                    note(report, "[browser] 使用本机 Edge 启动");
                } catch (PlaywrightException e) {
                    note(report, "[browser] Edge 启动失败，改用 Playwright Chromium: " + e.getMessage());
                    context = playwright.chromium().launchPersistentContext(profileDir, options);
                }

                context.onRequest(req -> {
                    if (req.url().contains("ttwid")) {
                        note(report, "[net-req] " + req.method() + " " + req.url());
                    }
                });
                context.onResponse(resp -> {
                    List<String> ttwidCookies = resp.headersArray().stream()
                            .filter(h -> "set-cookie".equalsIgnoreCase(h.name))
                            .map(h -> h.value)
                            .filter(v -> v.toLowerCase(Locale.ROOT).contains("ttwid"))
                            .collect(Collectors.toList());
                    if (!ttwidCookies.isEmpty() || resp.url().contains("ttwid")) {
                        note(report, "[net-resp] " + resp.status() + " " + resp.url());
                        ttwidCookies.forEach(v -> note(report, "[set-cookie-ttwid] " + v));
                    }
                });

                Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
                page.navigate(HOME, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(60_000));

                System.out.println("==========================================================");
                System.out.println("浏览器已打开。请在窗口中扫码或使用手机号登录抖音。");
                System.out.println("只有关注列表接口返回 status_code=0 才会导出 Cookie，最长等待 10 分钟。");
                System.out.println("若已显示登录但接口仍失败，请退出登录后重新扫码，并完成所有验证。");
                System.out.println("==========================================================");

                boolean loggedIn = waitForLogin(page, context, report, LOGIN_TIMEOUT_MS);
                if (!loggedIn) {
                    note(report, "[result] 10 分钟内登录态未通过接口验证，未覆盖 cookies.txt");
                    Files.write(reportFile, report, StandardCharsets.UTF_8);
                    context.close();
                    return false;
                }
                note(report, "[result] 登录态已通过接口验证（following status_code=0）");

                // 让登录后的页面完成一次会话初始化 / ttwid 刷新
                page.waitForTimeout(5_000);

                List<Cookie> cookies = context.cookies();
                reportCookies(cookies, report);
                writeCookieHeader(cookies, cookieFile, report);

                System.out.println();
                System.out.println("[test] 用当前登录态请求关注列表...");
                Object res = page.evaluate(
                        "async (arg) => {"
                                + "  const r = await fetch(arg.path + '?' + arg.query, {credentials:'include',"
                                + "      headers:{'accept':'application/json, text/plain, */*'}});"
                                + "  const t = await r.text();"
                                + "  return {status: r.status, len: t.length, head: t.slice(0, 300)};"
                                + "}",
                        Map.of("path", "/aweme/v1/web/user/following/list/", "query", FOLLOWING_QUERY));
                note(report, "[test-following] " + res);

                Files.write(reportFile, report, StandardCharsets.UTF_8);
                exported = true;
                System.out.println("[done] Cookie 已写入 " + cookieFile);
                System.out.println("[done] 报告已写入 " + reportFile);
                if (keepOpenMs > 0) {
                    System.out.println("[done] 浏览器将在 " + (keepOpenMs / 1000) + " 秒后自动关闭");
                    try {
                        page.waitForTimeout(keepOpenMs);
                    } catch (PlaywrightException ignored) {
                        // 用户提前关闭窗口
                    }
                }
                context.close();
            }
        } catch (Exception e) {
            note(report, "[warn] 登录流程异常: " + e);
            try {
                Files.write(reportFile, report, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
        return exported;
    }

    private static void note(List<String> report, String line) {
        System.out.println(line);
        report.add(line);
    }

    /**
     * 仅凭 sessionid 存在不可靠：风控会下发一个很短、但接口仍判定“未登录”的会话。
     * 因此必须用关注列表接口验证 status_code==0 才认为登录成功。
     */
    private static boolean waitForLogin(Page page, BrowserContext context, List<String> report,
                                        long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        long lastCheck = 0L;
        long lastStatus = Long.MIN_VALUE;
        while (System.currentTimeMillis() < deadline) {
            boolean hasCookie = hasLoginCookie(context.cookies());
            long now = System.currentTimeMillis();
            if (hasCookie && now - lastCheck >= 10_000L) {
                long status = apiFollowingStatus(page);
                if (status != lastStatus) {
                    note(report, "[login] sessionid 已出现，following status_code=" + status);
                    lastStatus = status;
                }
                if (status == 0L) {
                    return true;
                }
                lastCheck = now;
            }
            Thread.sleep(2_000L);
        }
        return false;
    }

    private static long apiFollowingStatus(Page page) {
        try {
            Object res = page.evaluate(
                    "async (arg) => {"
                            + "  const r = await fetch(arg.path + '?' + arg.query, {credentials:'include',"
                            + "      headers:{'accept':'application/json, text/plain, */*'}});"
                            + "  const t = await r.text();"
                            + "  try { const j = JSON.parse(t);"
                            + "    return j && j.status_code != null ? j.status_code : -1; }"
                            + "  catch (e) { return -1; }"
                            + "}",
                    Map.of("path", "/aweme/v1/web/user/following/list/", "query", FOLLOWING_QUERY));
            return res instanceof Number ? ((Number) res).longValue() : -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    private static boolean hasLoginCookie(List<Cookie> cookies) {
        return cookies.stream().anyMatch(c ->
                ("sessionid".equals(c.name) || "sessionid_ss".equals(c.name) || "sid_tt".equals(c.name))
                        && c.value != null && c.value.length() > 10);
    }

    private static void reportCookies(List<Cookie> cookies, List<String> report) {
        note(report, "[cookies] 共 " + cookies.size() + " 个");
        for (String name : List.of("ttwid", "sid_guard", "sessionid", "sessionid_ss", "sid_tt",
                "UIFID", "msToken", "passport_csrf_token")) {
            cookies.stream().filter(c -> name.equals(c.name)).findFirst().ifPresent(c -> {
                String value = "sid_guard".equals(name) ? decode(c.value) : abbreviate(c.value);
                note(report, "[cookie] " + name + " = " + value);
                if (c.expires != null && c.expires > 0) {
                    note(report, "[cookie] " + name + " expires = " + fmt(c.expires.longValue()));
                }
            });
        }
        cookies.stream().filter(c -> "sid_guard".equals(c.name)).findFirst().ifPresent(c -> {
            // sid_guard = sessionid|签发时间|有效期秒数|到期时间(GMT)
            String[] parts = decode(c.value).split("\\|");
            if (parts.length >= 4) {
                try {
                    long issued = Long.parseLong(parts[1]);
                    long maxAge = Long.parseLong(parts[2]);
                    ZonedDateTime expiry = ZonedDateTime.parse(parts[3],
                            DateTimeFormatter.RFC_1123_DATE_TIME);
                    long hours = Duration.between(ZonedDateTime.now(), expiry).toHours();
                    note(report, "[cookie] sid_guard 签发 = " + fmt(issued)
                            + "，有效期 " + (maxAge / 86400) + " 天，到期 = "
                            + expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + "（剩余 " + hours + " 小时）");
                } catch (RuntimeException ignored) {
                }
            }
        });
    }

    private static void writeCookieHeader(List<Cookie> cookies, Path file, List<String> report) throws Exception {
        String header = cookies.stream()
                .filter(c -> c.domain != null && c.domain.contains("douyin.com"))
                .sorted(Comparator.comparing(c -> c.name))
                .map(c -> c.name + "=" + c.value)
                .collect(Collectors.joining("; "));
        Files.writeString(file, header, StandardCharsets.UTF_8);
        note(report, "[cookies] 已导出 " + file + "（" + header.length() + " 字符）");
    }

    private static String decode(String v) {
        return URLDecoder.decode(v, StandardCharsets.UTF_8);
    }

    private static String abbreviate(String v) {
        if (v == null) {
            return "null";
        }
        return v.length() <= 24
                ? v
                : v.substring(0, 12) + "..." + v.substring(v.length() - 8) + " (len=" + v.length() + ")";
    }

    private static String fmt(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
