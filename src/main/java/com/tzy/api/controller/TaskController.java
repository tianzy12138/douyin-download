package com.tzy.api.controller;

import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final DownloadService downloadService;
    private final UserService userService;
    private final AtomicBoolean taskRunning = new AtomicBoolean(false);
    
    @PostMapping("/start")
    public String startDownloadTask() {
        if (taskRunning.get()) {
            return "任务正在运行中，请稍后再试";
        }
        new Thread(() -> {
            taskRunning.set(true);
            try {
                log.info("手动触发下载任务开始");
                userService.findEnabledUsers().forEach(user -> {
                    downloadService.downloadUserContent(user);
                });
                log.info("手动触发下载任务完成");
            } finally {
                taskRunning.set(false);
            }
        }).start();
        return "下载任务已启动";
    }
    
    @GetMapping("/status")
    public String getTaskStatus() {
        return taskRunning.get() ? "RUNNING" : "IDLE";
    }
}
