package com.example.banking.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final LoginSessionRepository sessionRepository;
    private final RefreshTokenRepository  refreshTokenRepository;

    @Transactional
    public LoginSession startSession(Long userId, String refreshToken, HttpServletRequest req) {
        String ua = req == null ? null : req.getHeader("User-Agent");
        LoginSession s = LoginSession.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .deviceId(DeviceParser.fingerprint(ua))
                .deviceName(DeviceParser.browserOf(ua) + " on " + DeviceParser.osOf(ua))
                .os(DeviceParser.osOf(ua))
                .browser(DeviceParser.browserOf(ua))
                .ipAddress(AuditService.clientIp(req))
                .userAgent(ua)
                .issuedAt(Instant.now())
                .lastActiveAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 30)) // 30d
                .active(true)
                .build();
        return sessionRepository.save(s);
    }

    @Transactional
    public void touch(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken).ifPresent(s -> {
            s.setLastActiveAt(Instant.now());
            sessionRepository.save(s);
        });
    }

    public List<LoginSession> activeSessions(Long userId) {
        return sessionRepository.findByUserIdAndActiveTrueOrderByLastActiveAtDesc(userId);
    }

    @Transactional
    public void terminateSession(String refreshToken) {
        sessionRepository.deactivateByRefreshToken(refreshToken);
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void terminateAllForUser(Long userId) {
        sessionRepository.deactivateAllForUser(userId);
        refreshTokenRepository.revokeAllForUser(userId);
    }
}
