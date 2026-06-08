package com.example.banking.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String ACTION_LOGIN_SUCCESS  = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAIL     = "LOGIN_FAIL";
    public static final String ACTION_LOGOUT         = "LOGOUT";
    public static final String ACTION_LOGOUT_ALL     = "LOGOUT_ALL";
    public static final String ACTION_TOKEN_REFRESH  = "TOKEN_REFRESH";
    public static final String ACTION_PASSWORD_CHANGE= "PASSWORD_CHANGE";
    public static final String ACTION_PASSWORD_RESET_REQ = "PASSWORD_RESET_REQUEST";
    public static final String ACTION_PASSWORD_RESET_DONE = "PASSWORD_RESET_COMPLETE";
    public static final String ACTION_MFA_ENABLED    = "MFA_ENABLED";
    public static final String ACTION_MFA_DISABLED   = "MFA_DISABLED";
    public static final String ACTION_ACCOUNT_LOCKED  = "ACCOUNT_LOCKED";
    public static final String ACTION_REGISTER       = "USER_REGISTERED";
    public static final String ACTION_RATE_LIMITED    = "RATE_LIMITED";

    private final AuditLogRepository repository;

    @Transactional
    public void log(Long userId, String username, String action, String severity, String details, HttpServletRequest req) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .severity(severity == null ? "INFO" : severity)
                .details(details)
                .ipAddress(clientIp(req))
                .userAgent(req == null ? null : req.getHeader("User-Agent"))
                .build();
        repository.save(log);
    }

    @Transactional
    public void log(Long userId, String username, String action, String details, HttpServletRequest req) {
        log(userId, username, action, "INFO", details, req);
    }

    public static String clientIp(HttpServletRequest req) {
        if (req == null) return null;
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
