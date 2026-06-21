package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class JsonRootBean {

    private Long status_code;
    private long min_cursor;
    private long max_cursor;
    private Long has_more;
    private List<AwemeList> aweme_list;
//    private String time_list;
//    private LogPb log_pb;
//    private long request_item_cursor;
//    private Long post_serial;
//    private Long replace_series_cover;


}