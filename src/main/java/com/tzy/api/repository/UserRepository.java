package com.tzy.api.repository;

import com.tzy.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByEnabledTrue();

    Optional<User> findBySecUid(String secUid);

    boolean existsBySecUid(String secUid);

    List<User> findByEnabledTrueAndLastPostTimeGreaterThanEqualAndSyncTimeLessThan(LocalDate lastPostTime, LocalDate syncTime);

    List<User> findByEnabledTrueAndLastPostTimeLessThanEqual(LocalDateTime lastPostTime);

    @Modifying
    @Query("UPDATE User u SET u.syncTime = :syncTime WHERE u.id = :id")
    int updateSyncTimeById(String id, LocalDate syncTime);

    List<User> findByEnabledTrueAndLastPostTimeIsNull();

}
