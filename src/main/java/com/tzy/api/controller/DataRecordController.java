package com.tzy.api.controller;

import com.tzy.api.entity.DataRecord;
import com.tzy.api.entity.User;
import com.tzy.api.service.DataRecordService;
import com.tzy.api.service.DownloadService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class DataRecordController {

    private final DataRecordService dataRecordService;
    private final DownloadService downloadService;

    @PostMapping("/pending")
    public Page<DataRecord> listPending(@RequestBody PageQuery query) {
        Sort sort = query.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(query.getSortBy()).ascending()
                : Sort.by(query.getSortBy()).descending();
        PageRequest pageRequest = PageRequest.of(query.getPage(), query.getSize(), sort);
        return dataRecordService.findPending(pageRequest);
    }

    @PutMapping("/{id}/downloaded")
    public DataRecord markAsDownloaded(@PathVariable String id) {
        return dataRecordService.markAsDownloaded(id);
    }

    @PostMapping("/by-sec-uid/{secUid}")
    public void addBySecUid(@PathVariable String secUid) {
        User user = new User();
        user.setShareUrl("https://www.douyin.com/user/" + secUid);
        downloadService.downloadUserContent(user);
    }

    @Data
    public static class PageQuery {
        private int page = 0;
        private int size = 20;
        private String sortBy = "publishTime";
        private String sortDir = "desc";
    }
}
