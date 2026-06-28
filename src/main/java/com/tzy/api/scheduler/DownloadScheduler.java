package com.tzy.api.scheduler;

import com.tzy.api.entity.User;
import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadScheduler {
    
    private final DownloadService downloadService;
    @Autowired
    @Lazy
    private UserService userService;

    @Scheduled(initialDelay = 1L, fixedDelay = 1L, timeUnit = TimeUnit.MINUTES)
    public void executeDownloadTask() {
        log.info("定时下载任务开始");
        for (User user : userService.findRandomEnabledUsers()) {
            downloadService.downloadUserContent(user);
            userService.updateSyncTime(user.getId());
        }
        log.info("定时下载任务完成");
    }

    @Scheduled(initialDelay = 1L, fixedDelay = 30L, timeUnit = TimeUnit.MINUTES)
    public void executeCollectsDiscovery() {
        userService.addUserByCollection();
    }
}
