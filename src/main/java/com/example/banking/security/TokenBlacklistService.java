package com.example.banking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository repository;
    private final JwtService jwtService;

    @Transactional
    public void blacklist(String token, Long userId, String reason) {
        if (token == null || token.isBlank() || repository.existsByToken(token)) return;
        Date exp = jwtService.extractExpiration(token);
        Instant expiresAt = exp == null ? Instant.now().plusSeconds(3600) : exp.toInstant();
        BlacklistedToken bl = BlacklistedToken.builder()
                .token(token)
                .userId(userId)
                .expiresAt(expiresAt)
                .blacklistedAt(Instant.now())
                .reason(reason)
                .build();
        repository.save(bl);
    }

    public boolean isBlacklisted(String token) {
        if (token == null) return false;
        return repository.existsByToken(token);
    }
}
