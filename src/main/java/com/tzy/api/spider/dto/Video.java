package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;


@Data
public class Video {

    private PlayAddr play_addr;
    private Cover cover;
    private Long height;
    private Long width;
    private DynamicCover dynamic_cover;
    private OriginCover origin_cover;
    private String ratio;
    private String format;
    private List<String> big_thumbs;
    private String meta;
    private List<BitRate> bit_rate;
    private Long duration;
    private String bit_rate_audio;
    private GaussianCover gaussian_cover;
    private PlayAddr265 play_addr_265;
    private Audio audio;
    private PlayAddrH264 play_addr_h264;
    private RawCover raw_cover;
    private AnimatedCover animated_cover;
    private Long is_source_HDR;
    private boolean use_static_cover;
    private String video_model;


}