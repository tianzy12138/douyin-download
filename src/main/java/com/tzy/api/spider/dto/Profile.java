package com.tzy.api.spider.dto;

import lombok.Data;

@Data
public class Profile {
    private User user;
    private Integer status_code;
    private String status_msg;
}
