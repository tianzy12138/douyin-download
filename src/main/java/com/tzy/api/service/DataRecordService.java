package com.tzy.api.service;

import com.tzy.api.common.DataType;
import com.tzy.api.common.DownloadStatus;
import com.tzy.api.entity.DataRecord;
import com.tzy.api.entity.Partner;
import com.tzy.api.repository.DataRecordRepository;
import com.tzy.api.spider.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRecordService {

    private final DataRecordRepository dataRecordRepository;

    @Autowired
    @Lazy
    private UserService userService;

    public Page<DataRecord> findPending(Pageable pageable) {
        return dataRecordRepository.findByStatus(DownloadStatus.PENDING, pageable);
    }

    public Optional<DataRecord> findByContentId(String contentId) {
        return dataRecordRepository.findByContentId(contentId);
    }

    public boolean existsByContentId(String contentId) {
        return dataRecordRepository.existsByContentId(contentId);
    }

    public DataRecord markAsDownloaded(String id) {
        DataRecord record = dataRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在: " + id));
        record.setStatus(DownloadStatus.DOWNLOADED);
        return dataRecordRepository.saveAndFlush(record);
    }

    @Async
    public void saveFromAwemeList(AwemeList aweme) {
        String awemeId = aweme.getAweme_id();
        boolean b = existsByContentId(awemeId);
        if (b) {
            return;
        }
        DataRecord record = new DataRecord();
        Author author = aweme.getAuthor();
        record.setUserId(author.getUid());
        record.setNickname(author.getNickname());
        record.setSecUid(author.getSec_uid());


        CooperationInfo cooperationInfo = aweme.getCooperation_info();
        if (Objects.nonNull(cooperationInfo) && CollectionUtils.isNotEmpty(cooperationInfo.getCo_creators())) {
            List<Partner> partners = cooperationInfo.getCo_creators().stream()
                    .map(this::convertToPartner)
                    .collect(Collectors.toList());
            record.setPartners(partners);
        }

        Video video = aweme.getVideo();
        List<Images> images = aweme.getImages();
        if (CollectionUtils.isNotEmpty(images)) {
            record.setDataType(DataType.IMAGE);
            List<String> imageUrls = images.stream()
                    .map(Images::getUrl_list)
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(Collection::stream)
                    .filter(o -> StringUtils.contains(o, ".jpeg?"))
                    .collect(Collectors.toList());
            record.setImageUrls(imageUrls);

            List<String> videoUrls = images.stream()
                    .map(Images::getVideo)
                    .filter(Objects::nonNull)
                    .map(Video::getPlay_addr)
                    .filter(Objects::nonNull)
                    .map(PlayAddr::getUrl_list)
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(Collection::stream)
                    .filter(j -> StringUtils.contains(j, "www.douyin.com"))
                    .collect(Collectors.toList());
            record.setVideoUrls(videoUrls);
        } else if (Objects.nonNull(video)) {
            record.setDataType(DataType.VIDEO);
            List<String> videoUrls;
            if (CollectionUtils.isEmpty(video.getBit_rate())) {
                videoUrls = new ArrayList<>();
            } else {
                videoUrls = video.getPlay_addr()
                        .getUrl_list()
                        .stream()
                        .filter(j -> StringUtils.contains(j, "www.douyin.com"))
                        .toList();
            }
            record.setVideoUrls(videoUrls);
        }

        record.setPublishTime(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(aweme.getCreate_time()),
                ZoneId.systemDefault()));
        record.setContentId(awemeId);
        record.setTitle(aweme.getDesc());
        record.setStatus(DownloadStatus.PENDING);
        record.setSourceUrl(aweme.getShare_url());
        userService.updateLastPostTime(record.getSecUid(), record.getPublishTime());
        dataRecordRepository.saveAndFlush(record);
    }

    private Partner convertToPartner(CoCreators coCreator) {
        Partner partner = new Partner();
        partner.setNickname(coCreator.getNickname());
        partner.setSecUid(coCreator.getSec_uid());
        return partner;
    }

}
