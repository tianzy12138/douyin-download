package com.tzy.api.repository;

import com.tzy.api.common.DownloadStatus;
import com.tzy.api.entity.DataRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataRecordRepository extends JpaRepository<DataRecord, String> {

    Page<DataRecord> findByStatus(DownloadStatus status, Pageable pageable);

    Optional<DataRecord> findByContentId(String contentId);

    boolean existsByContentId(String contentId);
}
