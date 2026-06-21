package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class PlayAddr {

    private String uri;
    private List<String> url_list;
    private Long width;
    private Long height;
    private String url_key;
    private long data_size;
    private String file_hash;
    private String file_cs;


}