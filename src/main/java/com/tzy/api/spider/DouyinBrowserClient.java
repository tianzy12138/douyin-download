package com.tzy.api.spider;

import com.alibaba.fastjson.JSON;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.tzy.api.spider.dto.JsonRootBean;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final Object pageLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DouyinBrowserClient(String cookie) {
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));
        this.context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(DESKTOP_UA)
                .setLocale("zh-CN")
                .setViewportSize(1920, 1080));
        if (cookie != null && !cookie.isBlank()) {
            context.addCookies(parseCookies(cookie));
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

    private static List<Cookie> parseCookies(String cookie) {
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
            Cookie c = new Cookie(pair.substring(0, eq), pair.substring(eq + 1));
            c.setDomain(".douyin.com");
            c.setPath("/");
            cookies.add(c);
        }
        return cookies;
    }

    /**
     * 在页面上下文请求作品列表。返回 null 表示响应为空/不可解析。
     */
    public JsonRootBean findPosts(long maxCursor, String secUserId, String aid) {
        String query = buildQuery(maxCursor, secUserId, aid);
        String body;
        synchronized (pageLock) {
            body = (String) page.evaluate(
                    "qs => fetch('/aweme/v1/web/aweme/post/?' + qs, "
                            + "{credentials:'include', headers:{'accept':'application/json, text/plain, */*'}})"
                            + ".then(r => r.text())",
                    query);
        }
        if (body == null || body.isBlank()) {
            log.warn("empty response: secUserId={}, maxCursor={}", secUserId, maxCursor);
            return null;
        }
        JsonRootBean bean = JSON.parseObject(body, JsonRootBean.class);
        if (bean == null) {
            log.warn("invalid json: {}", body.substring(0, Math.min(300, body.length())));
        }
        return bean;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
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
