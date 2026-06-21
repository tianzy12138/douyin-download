package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class Images {

    private String uri;
    private List<String> url_list;
    private List<String> download_url_list;
    private int height;
    private int width;
    private String mask_url_list;
    private int clip_type;
    private String interaction_stickers;
    private Video video;

}