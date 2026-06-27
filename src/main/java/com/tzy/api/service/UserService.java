package com.tzy.api.service;

import com.tzy.api.entity.User;
import com.tzy.api.repository.UserRepository;
import com.tzy.api.spider.dto.Author;
import com.tzy.api.spider.dto.AwemeList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DownloadService downloadService;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findEnabledUsers() {
        return userRepository.findByEnabledTrue();
    }

    public List<User> findRandomEnabledUsers() {
        List<User> byEnabledTrue = userRepository.findByEnabledTrueAndLastPostTimeIsNull();
        Collections.shuffle(byEnabledTrue);
        return byEnabledTrue.stream().limit(10).collect(Collectors.toList());
    }

    public List<User> findEnabledUsersByDays(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LocalDate today = LocalDate.now();
        return userRepository.findByEnabledTrueAndLastPostTimeGreaterThanEqualAndSyncTimeLessThanEqual(threshold, today);
    }

    public List<User> findNotPublishUsersByDays(int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return userRepository.findByEnabledTrueAndLastPostTimeLessThanEqual(threshold);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User addUser(String shareUrl, String nickname) {
        String secUid = extractSecUid(shareUrl);
        if (userRepository.existsBySecUid(secUid)) {
            throw new IllegalArgumentException("用户已存在: " + secUid);
        }
        User user = new User();
        user.setShareUrl(shareUrl);
        user.setSecUid(secUid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    @Transactional
    public User addUserIfNotExists(String shareUrl, String nickname) {
        String secUid = extractSecUid(shareUrl);
        Optional<User> existing = userRepository.findBySecUid(secUid);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = new User();
        user.setShareUrl(shareUrl);
        user.setSecUid(secUid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    public User addBySecUid(String secUid, String nickname) {
        User user = new User();
        String shareUrl = "http://www.douyin.com/user/" + secUid;
        user.setShareUrl(shareUrl);
        user.setSecUid(secUid);
        user.setNickname(nickname);
        return userRepository.save(user);
    }

    @Transactional
    public User updateEnabled(String id, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(String id) {
        userRepository.deleteById(id);
    }

    @Transactional
    @Async
    public void updateLastPostTime(String secUid, LocalDateTime lastPostTime) {
        userRepository.findBySecUid(secUid).ifPresent(user -> {
            if (Objects.isNull(user.getLastPostTime())
                    || lastPostTime.toLocalDate().isAfter(user.getLastPostTime())) {
                user.setLastPostTime(lastPostTime.toLocalDate());
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void updateSyncTime(String id) {
        userRepository.updateSyncTimeById(id, LocalDate.now());
    }

    private String extractSecUid(String shareUrl) {
        if (StringUtils.contains(shareUrl, "https://v.douyin.com/")) {
            throw new IllegalArgumentException("分享链接需要先解析，请使用完整用户主页链接");
        }
        if (StringUtils.contains(shareUrl, "?")) {
            return StringUtils.substringBetween(shareUrl, "https://www.douyin.com/user/", "?");
        }
        return StringUtils.substringAfterLast(shareUrl, "/");
    }

    public void addUserByCollection() {
        List<AwemeList> authors = downloadService.discoverFromCollects(null);
        if (CollectionUtils.isEmpty(authors)) {
            return;
        }
        List<User> existingUsers = findAll();
        Set<String> existingSecUids = existingUsers.stream()
                .map(User::getSecUid)
                .collect(Collectors.toSet());
        for (AwemeList aweme : authors) {
            Author author = aweme.getAuthor();
            if (!existingSecUids.contains(author.getSec_uid())) {
                addBySecUid(author.getSec_uid(), author.getNickname());
            }
//            downloadService.cancelCollect(aweme.getAweme_id());
        }
    }
}
