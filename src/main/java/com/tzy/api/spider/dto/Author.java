package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class Author {

    private String uid;
    private String ban_user_functions;
    private String nickname;
    private String cf_list;
    private String link_item_list;
    private AvatarThumb avatar_thumb;
    private String avatar_schema_list;
    private String signature_extra;
    private Long follow_status;
    private String risk_notice_text;
    private String private_relation_list;
    private String follower_list_secondary_information_struct;
    private String custom_verify;
    private String can_set_geofencing;
    private String batch_unfollow_contain_tabs;
    private String display_info;
    private String verification_permission_ids;
    private String need_poLongs;
    private ShareInfo share_info;
    private String familiar_visitor_user;
    private String homepage_bottom_toast;
    private String batch_unfollow_relation_desc;
    private String enterprise_verify_reason;
    private boolean is_ad_fake;
    private String account_cert_info;
    private String Longerest_tags;
    private String user_tags;
    private String profile_mob_params;
    private String card_entries_not_display;
    private String not_seen_item_id_list;
    private String card_entries;
    private boolean prevent_download;
    private String text_extra;
    private String sec_uid;
    private String im_role_ids;
    private Long follower_status;
    private String not_seen_item_id_list_v2;
    private String contrail_list;
    private String data_label_list;
    private List<CoverUrl> cover_url;
    private String user_permissions;
    private String offline_info_list;
    private String endorsement_info_list;
    private String card_sort_priority;
    private String personal_tag_list;
    private String white_cover_url;
    private String creator_tag_list;
    private String special_people_labels;


}