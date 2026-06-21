package com.tzy.api.spider.dto;

import lombok.Data;


@Data
public class AwemeControl {

    private boolean can_forward;
    private boolean can_share;
    private boolean can_comment;
    private boolean can_show_comment;


}