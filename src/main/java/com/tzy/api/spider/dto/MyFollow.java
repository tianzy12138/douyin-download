package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;

@Data
public class MyFollow {

    //    private Extra extra;
    private List<Followings> followings;
    private boolean has_more;
    private Long hotsoon_has_more;
    private String hotsoon_text;
    private LogPb log_pb;
    private long max_time;
    private long min_time;
    private Long mix_count;
    private String myself_user_id;
    private Long offset;
    private boolean rec_has_more;
    private Long status_code;
    private String store_page;
    private Long total;
    private Long vcd_count;
}