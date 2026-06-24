package com.tzy.api.scheduler;

import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadScheduler {
    
    private final DownloadService downloadService;
    private final UserService userService;
    
    @Scheduled(cron = "${app.schedule.download-cron}")
    public void executeDownloadTask() {
        log.info("定时下载任务开始");
        userService.findEnabledUsers().forEach(user -> {
            downloadService.downloadUserContent(user, false);
        });
        log.info("定时下载任务完成");
    }

    @Scheduled(fixedDelay = 1, fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void executeCollectsDiscovery() {
        userService.addUserByCollection();
    }
}
