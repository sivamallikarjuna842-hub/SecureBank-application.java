package com.example.banking.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    Page<LoginHistory> findByUserIdOrderByLoggedInAtDesc(Long userId, Pageable pageable);
    List<LoginHistory> findTop20ByUserIdOrderByLoggedInAtDesc(Long userId);
}
