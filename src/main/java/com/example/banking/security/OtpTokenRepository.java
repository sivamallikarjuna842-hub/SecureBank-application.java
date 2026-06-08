package com.example.banking.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(Long userId, String purpose);

    @Modifying @Transactional
    @Query("delete from OtpToken t where t.expiresAt < ?1")
    void deleteAllExpired(Instant cutoff);
}
