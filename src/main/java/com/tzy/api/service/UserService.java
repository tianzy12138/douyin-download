package com.tzy.api.service;

import com.tzy.api.entity.User;
import com.tzy.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    
    public List<User> findAll() {
        return userRepository.findAll();
    }
    
    public List<User> findEnabledUsers() {
        return userRepository.findByEnabledTrue();
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
    public void updateLastPostTime(String secUid, LocalDateTime lastPostTime) {
        userRepository.findBySecUid(secUid).ifPresent(user -> {
            user.setLastPostTime(lastPostTime);
            userRepository.save(user);
        });
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
}
