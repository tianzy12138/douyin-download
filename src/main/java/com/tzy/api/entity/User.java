package com.tzy.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 100)
    private String nickname;

    @Column(length = 200, unique = true)
    private String secUid;

    @Column(length = 500)
    private String shareUrl;

    private Boolean enabled = true;

    private LocalDateTime createdAt;

    private LocalDate lastPostTime;

    private LocalDate syncTime;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Strings.CI.remove(UUID.randomUUID().toString(), "-");
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
}
