package com.example.banking.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    @Modifying @Transactional
    @Query("update RefreshToken r set r.revoked = true where r.userId = ?1")
    void revokeAllForUser(Long userId);

    @Modifying @Transactional
    @Query("delete from RefreshToken r where r.expiresAt < ?1")
    void deleteAllExpired(Instant cutoff);
}
