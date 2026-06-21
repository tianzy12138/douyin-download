package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class GaussianCover {

    private String uri;
    private List<String> url_list;
    private Long width;
    private Long height;


}