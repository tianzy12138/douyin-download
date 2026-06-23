package com.tzy.api.controller;

import com.tzy.api.entity.User;
import com.tzy.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public List<User> listUsers() {
        return userService.findAll();
    }
    
    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody AddUserRequest request) {
        User user = userService.addUser(request.getShareUrl(), request.getNickname());
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        User user = userService.updateEnabled(id, request.getEnabled());
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @lombok.Data
    public static class AddUserRequest {
        private String shareUrl;
        private String nickname;
    }
    
    @lombok.Data
    public static class UpdateUserRequest {
        private Boolean enabled;
    }
}
