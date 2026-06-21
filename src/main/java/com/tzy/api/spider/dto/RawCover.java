package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class RawCover {

    private String uri;
    private Long width;
    private List<String> url_list;
    private Long height;


}