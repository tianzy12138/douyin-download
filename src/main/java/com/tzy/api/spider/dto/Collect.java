package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;

@Data
public class Collect {
    private List<AwemeList> aweme_list;
    private Long cursor;
    //    private Extra extra;
    private Integer has_more;
    //    private Log_pb log_pb;
    private String sec_uid;
    private Integer status_code;
    private String uid;
}
