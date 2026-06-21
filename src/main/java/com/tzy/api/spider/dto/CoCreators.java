package com.tzy.api.spider.dto;

import lombok.Data;

@Data
public class CoCreators {

    private String uid;
    private String sec_uid;
    private String nickname;
    private AvatarThumb avatar_thumb;
    private int role_id;
    private String role_title;
    private int invite_status;
    private int index;
    private int follow_status;
    private int follower_status;
    private String extra;
    private long follower_count;
    private String custom_verify;
    private String enterprise_verify_reason;
}