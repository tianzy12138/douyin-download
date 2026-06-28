package com.tzy.api.entity;

import com.tzy.api.common.DataType;
import com.tzy.api.common.DownloadStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "data_records")
public class DataRecord {

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 36)
    private String userId;

    @Column(length = 100)
    private String nickname;

    @Column(length = 200)
    private String secUid;

    @ElementCollection
    @CollectionTable(name = "data_record_partners", joinColumns = @JoinColumn(name = "data_record_id"))
    private List<Partner> partners = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DataType dataType;

    private LocalDateTime publishTime;

    @Column(length = 100)
    private String contentId;

    @Column(columnDefinition = "TEXT")
    private String title;

    @ElementCollection
    @CollectionTable(name = "data_record_images", joinColumns = @JoinColumn(name = "data_record_id"))
    @Column(name = "url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "data_record_videos", joinColumns = @JoinColumn(name = "data_record_id"))
    @Column(name = "url", length = 500)
    private List<String> videoUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DownloadStatus status;

    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Strings.CI.remove(UUID.randomUUID().toString(), "-");
        }
    }
}
