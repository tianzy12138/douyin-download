package com.tzy.api.spider;

import com.tzy.api.spider.dto.Collect;
import com.tzy.api.spider.dto.JsonRootBean;
import com.tzy.api.spider.dto.MyFollow;

/**
 * 临时集成自检：用无头浏览器请求作品列表、关注列表、收藏夹。
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
            System.out.println("posts   status=" + (bean == null ? null : bean.getStatus_code())
                    + " count=" + count
                    + " has_more=" + (bean == null ? null : bean.getHas_more()));
        } finally {
            tiktok.close();
        }

        try (DouyinBrowserClient client = new DouyinBrowserClient(Tiktok.COOKIE, false)) {
            com.tzy.api.spider.dto.Profile profile = client.findProfile(secUid);
            System.out.println("profile nick=" + (profile == null || profile.getUser() == null
                    ? null : profile.getUser().getNickname()));

            MyFollow follow = client.findMyFollow(0L, Tiktok.FOLLOW_USER_ID, Tiktok.FOLLOW_SEC_UID);
            int followCount = follow == null || follow.getFollowings() == null ? 0 : follow.getFollowings().size();
            System.out.println("follow  status=" + (follow == null ? null : follow.getStatus_code())
                    + " count=" + followCount
                    + " has_more=" + (follow != null && follow.isHas_more()));

            Collect collect = client.findCollects(0L, 7492594126609848114L);
            int collectCount = collect == null || collect.getAweme_list() == null ? 0 : collect.getAweme_list().size();
            System.out.println("collect status=" + (collect == null ? null : collect.getStatus_code())
                    + " count=" + collectCount
                    + " has_more=" + (collect == null ? null : collect.getHas_more()));
        }
    }
}
