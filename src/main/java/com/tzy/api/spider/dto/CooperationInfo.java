package com.tzy.api.spider.dto;

import lombok.Data;

import java.util.List;

@Data
public class CooperationInfo {

    private String tag;
    private String extra;
    private List<CoCreators> co_creators;
    private int co_creator_nums;
    private int accepted_nums;
    private int cursor;
}