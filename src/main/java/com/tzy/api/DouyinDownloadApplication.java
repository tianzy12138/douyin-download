package com.tzy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DouyinDownloadApplication {
    public static void main(String[] args) {
        SpringApplication.run(DouyinDownloadApplication.class, args);
    }
}
