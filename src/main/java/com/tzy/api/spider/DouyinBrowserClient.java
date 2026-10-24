package com.tzy.api.spider;

import com.alibaba.fastjson.JSON;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.tzy.api.spider.dto.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在无头浏览器的页面上下文里请求抖音 API。
 *
 * <p>抖音的 a_bogus 由 webmssdk/bdms 在页面内生成，纯 Java 没有稳定的公开入口。
 * 这里让浏览器加载一次抖音首页，之后在页面里 fetch：
 * 页面里的 secsdk/webmssdk 会自动补 uifid、timestamp、a_bogus、
 * x-secsdk-web-signature，服务端才会返回正常 JSON。
 */
@Slf4j
public final class DouyinBrowserClient implements AutoCloseable {

    public static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/155.0.0.0 Safari/537.36 Edg/155.0.0.0";

    private static final String HOME_URL = "https://www.douyin.com/jingxuan";

    /**
     * page.evaluate 在 Playwright Java 中是 NO_TIMEOUT，页面不响应时会永久阻塞。
     */
    private static final long EVALUATE_TIMEOUT_SECONDS = 15;

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final Object pageLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ExecutorService evaluator = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "playwright-evaluate");
        t.setDaemon(true);
        return t;
    });

    /**
     * 公开接口：丢弃旧 ttwid，让页面生成新的，避免被风控的 ttwid 导致空响应。
     */
    public DouyinBrowserClient(String cookie) {
        this(cookie, true);
    }

    /**
     * @param dropTtwid true 用于公开接口（作品列表）；false 用于登录接口（关注/收藏），
     *                  登录态需要 cookie 中原有的 ttwid
     */
    public DouyinBrowserClient(String cookie, boolean dropTtwid) {
        this.playwright = Playwright.create();
        // 使用完整 Chromium 的新 headless 模式：旧的 headless shell 会被抖音风控
        // 返回 HTTP 200 + 空 body，并让页面 SDK 卡死。
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setChannel("chromium")
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));
        this.context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(DESKTOP_UA)
                .setLocale("zh-CN")
                .setViewportSize(1920, 1080));
        if (cookie != null && !cookie.isBlank()) {
            context.addCookies(parseCookies(cookie, dropTtwid));
        }
        this.page = context.newPage();
        page.navigate(HOME_URL, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                .setTimeout(60_000));
        try {
            // secsdk 或 bdms/webmssdk 初始化后才具备请求签名能力
            page.waitForFunction("() => !!(window.SDKRuntime || window.byted_acrawler)", null,
                    new Page.WaitForFunctionOptions().setTimeout(30_000));
        } catch (PlaywrightException e) {
            log.warn("签名 SDK 未在 30s 内出现，仍尝试请求: {}", e.getMessage());
        }
        page.waitForTimeout(2_000);
    }

    private static String buildQuery(long maxCursor, String secUserId, String aid) {
        return "device_platform=webapp"
                + "&aid=" + aid
                + "&channel=channel_pc_web"
                + "&sec_user_id=" + secUserId
                + "&max_cursor=" + maxCursor
                + "&count=18"
                + "&publish_video_strategy_type=2"
                + "&from_user_page=1"
                + "&version_code=290100"
                + "&version_name=29.1.0"
                + "&pc_client_type=1"
                + "&pc_libra_divert=Windows"
                + "&support_h265=1&support_dash=1"
                + "&cpu_core_num=12&device_memory=32"
                + "&cookie_enabled=true"
                + "&screen_width=1920&screen_height=1080"
                + "&browser_language=zh-CN&browser_platform=Win32"
                + "&browser_name=Edge&browser_version=155.0.0.0"
                + "&browser_online=true"
                + "&engine_name=Blink&engine_version=155.0.0.0"
                + "&os_name=Windows&os_version=10"
                + "&platform=PC&downlink=1.5&effective_type=4g&round_trip_time=100";
    }

    private static List<Cookie> parseCookies(String cookie, boolean dropTtwid) {
        List<Cookie> cookies = new ArrayList<>();
        for (String item : cookie.split(";")) {
            String pair = item.trim();
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = pair.substring(0, eq);
            // 旧的 s_v_web_id（verifyFp）与当前页面生成的 fp 不匹配时，
            // 服务端会返回 HTTP 200 + 空 body；丢掉它，由页面重新生成。
            if ("s_v_web_id".equalsIgnoreCase(name)) {
                continue;
            }
            // 旧 ttwid 被风控标记后，公开接口会返回 HTTP 200 + 空 body；
            // 不带它，页面会自动生成新的 ttwid。登录接口则需要保留原 ttwid。
            if (dropTtwid && "ttwid".equalsIgnoreCase(name)) {
                continue;
            }
            Cookie c = new Cookie(name, pair.substring(eq + 1));
            c.setDomain(".douyin.com");
            c.setPath("/");
            cookies.add(c);
        }
        return cookies;
    }

    private static void hintLoginIfNeeded(String path) {
        if (path.contains("/following/") || path.contains("/collects/")) {
            log.warn("登录接口空响应：COOKIE 可能已过期或被风控，请重新登录后更新 cookie");
        }
    }

    /**
     * 在页面上下文请求作品列表。返回 null 表示响应为空/不可解析。
     */
    public JsonRootBean findPosts(long maxCursor, String secUserId, String aid) {
        String query = buildQuery(maxCursor, secUserId, aid);
        String body = fetchJson("/aweme/v1/web/aweme/post/", query);
        if (body == null) {
            log.warn("empty response: secUserId={}, maxCursor={}", secUserId, maxCursor);
            return null;
        }
        return JSON.parseObject(body, JsonRootBean.class);
    }

    /**
     * 关注列表（当前登录用户）。maxCursor 对应接口的 max_time。
     */
    public MyFollow findMyFollow(long maxCursor, String userId, String secUserId) {
        String query = "device_platform=webapp&aid=6383&channel=channel_pc_web"
                + "&user_id=" + userId
                + "&sec_user_id=" + secUserId
                + "&pc_client_type=1&is_top=1&address_book_access=0&gps_access=0&source_type=1"
                + "&update_version_code=170400&version_code=170400&version_name=17.4.0"
                + "&count=20&max_time=" + maxCursor
                + "&browser_online=true&cookie_enabled=true&os_name=Windows"
                + "&screen_width=1920&screen_height=1080"
                + "&browser_language=zh-CN&browser_platform=Win32"
                + "&browser_name=Edge&browser_version=155.0.0.0"
                + "&engine_name=Blink&engine_version=155.0.0.0"
                + "&os_version=10&cpu_core_num=12&device_memory=32"
                + "&platform=PC&downlink=1.5&effective_type=4g&round_trip_time=100";
        String body = fetchJson("/aweme/v1/web/user/following/list/", query);
        return body == null ? null : JSON.parseObject(body, MyFollow.class);
    }

    /**
     * 收藏夹视频列表。maxCursor 对应接口的 cursor。
     */
    public Collect findCollects(long maxCursor, long collectsId) {
        String query = "device_platform=webapp&aid=6383&channel=channel_pc_web"
                + "&collects_id=" + collectsId
                + "&cursor=" + maxCursor
                + "&count=20"
                + "&update_version_code=170400&pc_client_type=1&pc_libra_divert=Windows"
                + "&support_h265=1&support_dash=1&version_code=170400&version_name=17.4.0"
                + "&browser_language=zh-CN&browser_platform=Win32"
                + "&browser_name=Edge&browser_version=155.0.0.0"
                + "&engine_name=Blink&engine_version=155.0.0.0"
                + "&os_version=10&cpu_core_num=12&device_memory=32&platform=PC"
                + "&downlink=1.5&effective_type=4g&round_trip_time=100"
                + "&cookie_enabled=true&screen_width=1920&screen_height=1080"
                + "&browser_online=true&os_name=Windows";
        String body = fetchJson("/aweme/v1/web/collects/video/list/", query);
        return body == null ? null : JSON.parseObject(body, Collect.class);
    }

    /**
     * 其他用户主页信息。
     */
    public Profile findProfile(String secUserId) {
        String secUid = secUserId == null ? "" : secUserId;
        int userIdx = secUid.indexOf("/user/");
        if (userIdx >= 0) {
            secUid = secUid.substring(userIdx + "/user/".length());
        }
        int queryIdx = secUid.indexOf('?');
        if (queryIdx >= 0) {
            secUid = secUid.substring(0, queryIdx);
        }
        // 与浏览器实际请求保持一致；webid/uifid/msToken/a_bogus 由页面上下文补
        String query = "device_platform=webapp&aid=6383&channel=channel_pc_web"
                + "&publish_video_strategy_type=2&source=channel_pc_web"
                + "&sec_user_id=" + secUid
                + "&personal_center_strategy=1&profile_other_record_enable=1&land_to=1"
                + "&update_version_code=170400&pc_client_type=1&pc_libra_divert=Windows"
                + "&support_h265=1&support_dash=1&cpu_core_num=12"
                + "&version_code=170400&version_name=17.4.0"
                + "&cookie_enabled=true&screen_width=1920&screen_height=1080"
                + "&browser_language=zh-CN&browser_platform=Win32"
                + "&browser_name=Edge&browser_version=155.0.0.0"
                + "&browser_online=true&engine_name=Blink&engine_version=155.0.0.0"
                + "&os_name=Windows&os_version=10&device_memory=32&platform=PC"
                + "&downlink=10&effective_type=4g&round_trip_time=50";
        String body = fetchJson("/aweme/v1/web/user/profile/other/", query,
                "https://www.douyin.com/user/" + secUid);
        if (body == null) {
            return null;
        }
        Profile profile = JSON.parseObject(body, Profile.class);
        if (profile == null) {
            log.info(body);
        }
        return profile;
    }

    /**
     * 关注 / 取关。返回接口原始响应，空响应返回 null。
     */
    public String follow(String uid, boolean follow) {
        String query = "device_platform=webapp&aid=6383&channel=channel_pc_web"
                + "&pc_client_type=1&pc_libra_divert=Windows&update_version_code=170400"
                + "&support_h265=1&support_dash=1&version_code=170400&version_name=17.4.0"
                + "&cookie_enabled=true&screen_width=1920&screen_height=1080"
                + "&browser_language=zh-CN&browser_platform=Win32"
                + "&browser_name=Edge&browser_version=155.0.0.0"
                + "&browser_online=true&engine_name=Blink&engine_version=155.0.0.0"
                + "&os_name=Windows&os_version=10&cpu_core_num=12&device_memory=32"
                + "&platform=PC&downlink=1.5&effective_type=4g&round_trip_time=100";
        String form = "type=" + (follow ? "1" : "0") + "&user_id=" + uid;
        return postForm("/aweme/v1/web/commit/follow/user/", query, form);
    }

    /**
     * 取关所有已记录在文件里的关注用户。
     */
    public void cancelFollow(Set<String> recordedLines, List<Followings> followings) {
        for (Followings f : followings) {
            String secUid = f.getSec_uid();
            boolean recorded = secUid != null && recordedLines.stream()
                    .anyMatch(line -> line != null && line.contains(secUid));
            if (recorded) {
                follow(f.getUid(), false);
            }
        }
    }

    /**
     * 带超时的 page.evaluate。超时后关闭浏览器，调用方下次 browserClient() 会自动重建。
     */
    private Object evaluate(String script, Object arg) {
        Future<Object> future = evaluator.submit(() -> page.evaluate(script, arg));
        try {
            return future.get(EVALUATE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("page.evaluate 超时 {}s，异步关闭浏览器等待重建", EVALUATE_TIMEOUT_SECONDS);
            // close() 本身也可能卡在无响应的 driver 上，不能阻塞当前线程
            closed.set(true);
            Thread closer = new Thread(this::cleanup, "playwright-close");
            closer.setDaemon(true);
            closer.start();
            throw new PlaywrightException("page.evaluate timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlaywrightException("page.evaluate interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof PlaywrightException playwrightException) {
                throw playwrightException;
            }
            throw new PlaywrightException(cause == null ? "evaluate failed" : cause.toString());
        }
    }

    public boolean isClosed() {
        return closed.get();
    }

    /**
     * 在页面上下文执行 GET，由页面 SDK 自动补 uifid/timestamp/a_bogus/x-secsdk。
     * 返回 null 表示空响应或非 JSON（通常是风控软拒绝）。
     */
    private String fetchJson(String path, String query) {
        return fetchJson(path, query, null);
    }

    private String fetchJson(String path, String query, String referrer) {
        String body;
        synchronized (pageLock) {
            try {
                // fetch 挂起时 page.evaluate 没有超时，会永久卡住并占住 pageLock
                body = (String) evaluate(
                        "async arg => {"
                                + "  const c = new AbortController();"
                                + "  const t = setTimeout(() => c.abort(), 15000);"
                                + "  try {"
                                + "    const cookies = {};"
                                + "    document.cookie.split(';').forEach(s => {"
                                + "      const i = s.indexOf('=');"
                                + "      if (i > 0) cookies[s.slice(0, i).trim()] = s.slice(i + 1).trim();"
                                + "    });"
                                + "    const uifid = cookies['UIFID'] || '';"
                                + "    const webid = cookies['webid'] || localStorage.getItem('webid')"
                                + "      || (window.SDKRuntime && window.SDKRuntime.webid) || '';"
                                + "    const msToken = cookies['msToken'] || localStorage.getItem('msToken') || '';"
                                + "    let q = arg.query;"
                                + "    if (uifid) q += '&uifid=' + encodeURIComponent(uifid);"
                                + "    if (webid) q += '&webid=' + encodeURIComponent(webid);"
                                + "    if (msToken) q += '&msToken=' + encodeURIComponent(msToken);"
                                + "    const headers = {'accept': 'application/json, text/plain, */*'};"
                                + "    if (uifid) headers['uifid'] = uifid;"
                                + "    const opts = {credentials: 'include', signal: c.signal, headers};"
                                + "    if (arg.referrer) opts.referrer = arg.referrer;"
                                + "    const r = await fetch(arg.path + '?' + q, opts);"
                                + "    return await r.text();"
                                + "  } finally { clearTimeout(t); }"
                                + "}",
                        Map.of("path", path, "query", query,
                                "referrer", referrer == null ? "" : referrer));
            } catch (PlaywrightException e) {
                log.warn("fetch failed/timeout: path={}, {}", path, e.getMessage());
                hintLoginIfNeeded(path);
                return null;
            }
        }
        if (body == null || body.isBlank()) {
            log.warn("empty response: path={}", path);
            hintLoginIfNeeded(path);
            return null;
        }
        String head = body.trim();
        if (!head.startsWith("{")) {
            log.warn("non-json response: path={}, head={}", path,
                    head.substring(0, Math.min(300, head.length())));
            hintLoginIfNeeded(path);
            return null;
        }
        return body;
    }

    /**
     * 在页面上下文执行 form POST，由页面 SDK 自动补签名。
     * 返回 null 表示空响应。
     */
    private String postForm(String path, String query, String form) {
        String body;
        synchronized (pageLock) {
            try {
                body = (String) evaluate(
                        "async (arg) => {"
                                + "  const c = new AbortController();"
                                + "  const t = setTimeout(() => c.abort(), 15000);"
                                + "  try {"
                                + "    const r = await fetch(arg.path + '?' + arg.query, {"
                                + "      method: 'POST', credentials: 'include', signal: c.signal,"
                                + "      headers: {'accept': 'application/json, text/plain, */*',"
                                + "                'content-type': 'application/x-www-form-urlencoded',"
                                + "                'x-secsdk-csrf-token': 'DOWNGRADE'},"
                                + "      body: arg.form"
                                + "    });"
                                + "    return await r.text();"
                                + "  } finally { clearTimeout(t); }"
                                + "}",
                        Map.of("path", path, "query", query, "form", form));
            } catch (PlaywrightException e) {
                log.warn("POST failed/timeout: path={}, {}", path, e.getMessage());
                return null;
            }
        }
        if (body == null || body.isBlank()) {
            log.warn("empty response: POST {}", path);
            return null;
        }
        return body;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cleanup();
    }

    private void cleanup() {
        evaluator.shutdownNow();
        try {
            browser.close();
        } catch (Exception e) {
            log.warn("close browser error: {}", e.getMessage());
        }
        try {
            playwright.close();
        } catch (Exception e) {
            log.warn("close playwright error: {}", e.getMessage());
        }
    }
}
