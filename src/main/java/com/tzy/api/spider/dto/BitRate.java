package com.tzy.api.spider.dto;

import lombok.Data;


@Data
public class BitRate {

    private String gear_name;
    private Long quality_type;
    private long bit_rate;
    private PlayAddr play_addr;
    private Long is_h265;
    private Long is_bytevc1;
    private String HDR_type;
    private String HDR_bit;
    private Long FPS;
    private String video_extra;
    private String format;


}