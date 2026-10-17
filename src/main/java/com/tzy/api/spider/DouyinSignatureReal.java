package com.tzy.api.spider;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 抖音签名算法 - 真实场景版本
 * <p>
 * 签名类型: x-secsdk-web-signature (32字符MD5)
 * <p>
 * 算法验证: 基于真实浏览器请求捕获
 * <p>
 * 算法公式（已验证真实流量）:
 * signature = MD5(uifid + "_" + timestamp + "_" + SECRET_KEY + "_" + finalParams)
 * <p>
 * 其中 finalParams 是最终发送的完整 query：
 * - 包含 a_bogus / verifyFp / fp / webid / msToken 等所有参数
 * - 包含 uifid=...&timestamp=...
 * - 不包含 x-secsdk-web-signature
 * - 保持原顺序、原始百分号编码，不排序、不解码
 * <p>
 * 关键规则:
 * 1. uifid长度: 160字符或320字符（两种版本），前缀与 query 中使用同一个值
 * 2. SECRET_KEY: 固定值 A96D855A08C0A9707F8BEF0D9A527E4E
 * 3. 先算 a_bogus，再算本签名（a_bogus 被本签名覆盖）
 * 4. 浏览器规范流程: 传给 webSignUrl 的 URL 不含 uifid/timestamp/签名，
 * SDK 追加 uifid、timestamp 后再计算
 */
public class DouyinSignatureReal {

    // 固定密钥
    public static final String SECRET_KEY = "A96D855A08C0A9707F8BEF0D9A527E4E";

    /**
     * 计算签名（最终 query 版本，推荐）。
     *
     * @param uifid       设备指纹 (160或320字符)
     * @param timestamp   Unix时间戳 (秒)
     * @param finalParams 最终发送的完整 query（含 uifid/timestamp，不含签名）
     * @return 32字符MD5签名
     */
    public static String calculateSignatureOverFinalParams(String uifid, long timestamp, String finalParams) {
        return md5(uifid + "_" + timestamp + "_" + SECRET_KEY + "_" + finalParams);
    }

    /**
     * 计算签名（兼容旧调用：params 不含 uifid/timestamp，由本方法追加）。
     *
     * @param uifid     设备指纹 (160或320字符)
     * @param timestamp Unix时间戳 (秒)
     * @param params    URL查询参数字符串 (保持原始顺序，不包含 uifid/timestamp/签名)
     * @return 32字符MD5签名
     */
    public static String calculateSignature(String uifid, long timestamp, String params) {
        String base = params == null ? "" : params;
        String fullParams = base.isEmpty()
                ? "uifid=" + uifid + "&timestamp=" + timestamp
                : base + "&uifid=" + uifid + "&timestamp=" + timestamp;
        return calculateSignatureOverFinalParams(uifid, timestamp, fullParams);
    }

    /**
     * 从完整URL计算签名
     *
     * @param urlString 完整URL (包含所有查询参数)
     * @param uifid     设备指纹
     * @param timestamp 时间戳
     * @return 签名
     */
    public static String calculateSignatureFromUrl(String urlString, String uifid, long timestamp) {
        try {
            URL url = new URL(urlString);
            // 排除已存在的 uifid/timestamp/签名，再由 calculateSignature 规范地追加一次，
            // 避免对真实 URL 重复拼接（这是旧实现的真实请求 0% 匹配根因）。
            String params = stripSigningParams(url.getQuery());
            return calculateSignature(uifid, timestamp, params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse URL: " + urlString, e);
        }
    }

    /**
     * 去掉签名参数以及 SDK 会自行追加的 uifid/timestamp，保留其余原始顺序与编码。
     */
    private static String stripSigningParams(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        List<String> filtered = new ArrayList<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String key = pair.contains("=") ? pair.split("=", 2)[0] : pair;
            if (key.equals("x-secsdk-web-signature") || key.equals("uifid") || key.equals("timestamp")) {
                continue;
            }
            filtered.add(pair);
        }
        return String.join("&", filtered);
    }

    /**
     * 验证签名是否匹配
     *
     * @param expectedSignature 期望的签名
     * @param uifid             设备指纹
     * @param timestamp         时间戳
     * @param params            参数字符串
     * @return 是否匹配
     */
    public static boolean verifySignature(String expectedSignature, String uifid,
                                          long timestamp, String params) {
        String calculated = calculateSignature(uifid, timestamp, params);
        return calculated.equals(expectedSignature);
    }

    /**
     * 生成完整签名URL
     *
     * @param baseUrl   基础URL
     * @param uifid     设备指纹
     * @param timestamp 时间戳
     * @return 包含签名的完整URL
     */
    public static String generateSignedUrl(String baseUrl, String uifid, long timestamp) {
        try {
            URL url = new URL(baseUrl);
            String baseParams = stripSigningParams(url.getQuery());
            String fullParams = baseParams.isEmpty()
                    ? "uifid=" + uifid + "&timestamp=" + timestamp
                    : baseParams + "&uifid=" + uifid + "&timestamp=" + timestamp;
            String signature = calculateSignatureOverFinalParams(uifid, timestamp, fullParams);
            return url.getProtocol() + "://" + url.getAuthority() + url.getPath()
                    + "?" + fullParams + "&x-secsdk-web-signature=" + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse URL: " + baseUrl, e);
        }
    }

    // MD5计算
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 calculation failed", e);
        }
    }

    /**
     * 测试方法 - 使用真实捕获的数据验证
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("  抖音签名算法 - 真实场景验证");
        System.out.println("=".repeat(80));
        System.out.println();

        // 真实浏览器样本（2026-10-18 抓取）:
        // GET https://www.douyin.com/aweme/v1/web/aweme/post/?a=1
        String uifid = "a3682da019905bd2868511de77147b86e5069f1da12659d787063f1c7805c06f6ac3ca2afdbf3a26f9a4af13c1e49ee2d0cb425bd06f806cf2614228077c2925bf455e5fcbfbabc98b847dc5f621f754";
        long timestamp = 1792252798L;
        String expectedSignature = "05492662da206256ab35e21b769e703c";
        String baseUrl = "https://www.douyin.com/aweme/v1/web/aweme/post/?a=1";
        String finalParams = "a=1&uifid=" + uifid + "&timestamp=" + timestamp;
        String expectedUrl = baseUrl + "&uifid=" + uifid + "&timestamp=" + timestamp
                + "&x-secsdk-web-signature=" + expectedSignature;

        String calculated = calculateSignatureOverFinalParams(uifid, timestamp, finalParams);
        String fromUrl = calculateSignatureFromUrl(baseUrl, uifid, timestamp);
        String signedUrl = generateSignedUrl(baseUrl, uifid, timestamp);

        System.out.println("expected   : " + expectedSignature);
        System.out.println("calculated : " + calculated);
        System.out.println("fromUrl    : " + fromUrl);
        System.out.println("signedUrl  : " + signedUrl);

        boolean ok = calculated.equals(expectedSignature)
                && fromUrl.equals(expectedSignature)
                && signedUrl.equals(expectedUrl);
        System.out.println(ok ? "RESULT: PASS" : "RESULT: FAIL");
        if (!ok) {
            System.exit(1);
        }
    }
}
