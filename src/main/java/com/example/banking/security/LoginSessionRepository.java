package com.example.banking.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    List<LoginSession> findByUserIdAndActiveTrueOrderByLastActiveAtDesc(Long userId);
    Optional<LoginSession> findByRefreshToken(String refreshToken);

    @Modifying @Transactional
    @Query("update LoginSession s set s.active = false where s.userId = ?1")
    void deactivateAllForUser(Long userId);

    @Modifying @Transactional
    @Query("update LoginSession s set s.active = false where s.refreshToken = ?1")
    void deactivateByRefreshToken(String refreshToken);

    @Modifying @Transactional
    @Query("delete from LoginSession s where s.expiresAt < ?1")
    void deleteAllExpired(Instant cutoff);
}
