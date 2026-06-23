package com.tzy.api.repository;

import com.tzy.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    List<User> findByEnabledTrue();
    
    Optional<User> findBySecUid(String secUid);
    
    boolean existsBySecUid(String secUid);
}
