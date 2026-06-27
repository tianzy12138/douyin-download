package com.tzy.api.scheduler;

import com.tzy.api.entity.User;
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
        for (User user : userService.findEnabledUsers()) {
            downloadService.downloadUserContent(user, false);
            userService.updateSyncTime(user.getId());
        }
        log.info("定时下载任务完成");
    }

    @Scheduled(initialDelay = 1L, fixedDelay = 30L, timeUnit = TimeUnit.MINUTES)
    public void executeCollectsDiscovery() {
        userService.addUserByCollection();
    }
}
