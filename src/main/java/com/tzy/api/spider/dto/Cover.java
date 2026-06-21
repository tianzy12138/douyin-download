package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class Cover {

    private String uri;
    private List<String> url_list;
    private Long width;
    private Long height;


}