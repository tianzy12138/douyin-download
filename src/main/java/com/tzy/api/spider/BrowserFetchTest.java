package com.tzy.api.spider;

import com.tzy.api.spider.dto.JsonRootBean;

/**
 * 临时集成自检：用无头浏览器请求作品列表。
 */
public final class BrowserFetchTest {
    private BrowserFetchTest() {
    }

    public static void main(String[] args) {
        String secUid = args.length > 0 ? args[0] : "MS4wLjABAAAAY2f0wZ7xnzaTs115NVIGxVREicyWt92lPJbW8VT7JLc";
        Tiktok tiktok = new Tiktok();
        try {
            JsonRootBean bean = tiktok.findPosts(0L, secUid, "6383");
            int count = bean == null || bean.getAweme_list() == null ? 0 : bean.getAweme_list().size();
            System.out.println("status=" + (bean == null ? null : bean.getStatus_code())
                    + " count=" + count
                    + " has_more=" + (bean == null ? null : bean.getHas_more()));
        } finally {
            tiktok.close();
        }
    }
}
