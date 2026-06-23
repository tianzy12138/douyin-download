package com.tzy.api.scheduler;

import com.tzy.api.entity.User;
import com.tzy.api.service.DownloadService;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    @Scheduled(cron = "${app.schedule.collects-cron}")
    public void executeCollectsDiscovery() {
        log.info("收藏夹发现任务开始");
        Long collectsId = 7492594126609848114L;
        List<User> existingUsers = userService.findAll();
        Set<String> existingSecUids = existingUsers.stream()
                .map(User::getSecUid)
                .collect(Collectors.toSet());
        
        downloadService.discoverFromCollects(collectsId).forEach(author -> {
            if (!existingSecUids.contains(author.getSec_uid())) {
                String shareUrl = "http://www.douyin.com/user/" + author.getSec_uid();
                userService.addUserIfNotExists(shareUrl, author.getNickname());
                log.info("发现新用户: {}", author.getNickname());
            }
        });
        log.info("收藏夹发现任务完成");
    }
}
