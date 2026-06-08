package com.example.banking.user;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.RoleType;
import com.example.banking.security.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SessionService sessionService;
    private final TokenBlacklistService blacklistService;
    private final RateLimiter rateLimiter;

    @Value("${app.fraud.max-failed-login-attempts:3}")
    private int maxFailedAttempts;

    @Value("${app.fraud.lock-minutes:15}")
    private int lockMinutes;

    @Value("${app.auth.require-mfa:false}")
    private boolean requireMfa;

    @Value("${app.auth.reset-link-base:http://localhost:3001/reset-password?token=}")
    private String resetLinkBase;

    private static final SecureRandom RNG = new SecureRandom();

    /* ======================================================================
       REGISTRATION
       ====================================================================== */
    @Transactional
    public ApiResponse<Map<String, Object>> register(String fullName, String email, String username, String password, HttpServletRequest req) {
        if (userRepository.existsByEmail(email))    return ApiResponse.error("Email already registered");
        if (userRepository.existsByUsername(username)) return ApiResponse.error("Username already taken");

        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        if (password.length() < 6) return ApiResponse.error("Password must be at least 6 characters");
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            return ApiResponse.error("Password must contain letters and numbers");
        }

        User user = User.builder()
                .fullName(fullName).email(email).username(username)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .mfaEnabled(false).mfaChannel("EMAIL")
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(user);

        auditService.log(user.getId(), username, AuditService.ACTION_REGISTER, "New user registered", req);
        return ApiResponse.ok("Registration successful", Map.of(
                "userId", user.getId(),
                "username", username,
                "email", email
        ));
    }

    /* ======================================================================
       LOGIN  (step 1: password -> maybe MFA challenge or final tokens)
       ====================================================================== */
    @Transactional
    public ApiResponse<Map<String, Object>> login(String username, String password, String deviceId, HttpServletRequest req) {
        String ip = AuditService.clientIp(req);

        // Rate-limit per IP
        if (!rateLimiter.allow("login:ip:" + ip, 20, 60)) {
            recordHistory(null, username, "BLOCKED", req, "Rate limited");
            auditService.log(null, username, AuditService.ACTION_RATE_LIMITED, "WARN", "Login rate limited", req);
            return ApiResponse.error("Too many login attempts. Please try again later.");
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            recordHistory(null, username, "FAILURE", req, "User not found");
            auditService.log(null, username, AuditService.ACTION_LOGIN_FAIL, "WARN", "User not found", req);
            return ApiResponse.error("Invalid credentials");
        }
        if (!user.isEnabled()) {
            recordHistory(user.getId(), username, "BLOCKED", req, "Account disabled");
            return ApiResponse.error("Account is disabled");
        }
        if (user.isAccountLocked() && user.getLockTime() != null &&
                user.getLockTime().plusMinutes(lockMinutes).isAfter(LocalDateTime.now())) {
            recordHistory(user.getId(), username, "LOCKED", req, "Account locked");
            return ApiResponse.error("Account is locked. Try again in " + lockMinutes + " minutes.");
        }
        if (user.isAccountLocked() && user.getLockTime() != null &&
                user.getLockTime().plusMinutes(lockMinutes).isBefore(LocalDateTime.now())) {
            // Auto-unlock
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (BadCredentialsException e) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                auditService.log(user.getId(), username, AuditService.ACTION_ACCOUNT_LOCKED,
                        "CRITICAL", "Account locked after " + maxFailedAttempts + " failed attempts", req);
            }
            userRepository.save(user);
            recordHistory(user.getId(), username, "FAILURE", req, "Bad credentials (" + user.getFailedLoginAttempts() + ")");
            auditService.log(user.getId(), username, AuditService.ACTION_LOGIN_FAIL, "WARN",
                    "Bad credentials (attempt " + user.getFailedLoginAttempts() + ")", req);
            return ApiResponse.error("Invalid credentials");
        }

        // Password OK - reset counters
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);

        // If MFA is on, issue OTP and require verification before issuing tokens
        boolean mfaRequired = user.isMfaEnabled() || requireMfa;
        if (mfaRequired) {
            String code = otpService.issueOtp(user.getId(), user.getEmail(), OtpService.PURPOSE_LOGIN, user.getMfaChannel());
            recordHistory(user.getId(), username, "MFA_REQUIRED", req, "OTP issued via " + user.getMfaChannel());
            return ApiResponse.ok("MFA required", Map.of(
                    "mfaRequired", true,
                    "channel", user.getMfaChannel(),
                    "challengeId", UUID.randomUUID().toString(),
                    "devCode", code  // only for dev - never expose in prod
            ));
        }

        return finalizeLogin(user, req, "Login successful");
    }

    /* ======================================================================
       LOGIN  (step 2: verify MFA OTP -> tokens)
       ====================================================================== */
    @Transactional
    public ApiResponse<Map<String, Object>> verifyLoginOtp(String username, String code, HttpServletRequest req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        if (!otpService.verifyOtp(user.getId(), code, OtpService.PURPOSE_LOGIN)) {
            recordHistory(user.getId(), username, "FAILURE", req, "Bad MFA OTP");
            auditService.log(user.getId(), username, AuditService.ACTION_LOGIN_FAIL, "WARN", "Bad MFA OTP", req);
            return ApiResponse.error("Invalid or expired verification code");
        }
        return finalizeLogin(user, req, "Login successful");
    }

    private ApiResponse<Map<String, Object>> finalizeLogin(User user, HttpServletRequest req, String msg) {
        String role = user.getRoles().iterator().next().getName().name();
        String access  = jwtService.generateAccessToken(user.getId(), user.getUsername(), role);
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        RefreshToken rt = RefreshToken.builder()
                .token(refresh)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .revoked(false)
                .ipAddress(AuditService.clientIp(req))
                .userAgent(req == null ? null : req.getHeader("User-Agent"))
                .build();
        refreshTokenRepository.save(rt);
        sessionService.startSession(user.getId(), refresh, req);

        recordHistory(user.getId(), user.getUsername(), "SUCCESS", req, null);
        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_LOGIN_SUCCESS, "INFO", "Login OK", req);

        // Send login alert (best-effort)
        emailService.sendLoginAlert(user.getEmail(),
                AuditService.clientIp(req),
                DeviceParser.browserOf(req.getHeader("User-Agent")) + " on " + DeviceParser.osOf(req.getHeader("User-Agent")),
                LocalDateTime.now().toString());

        return ApiResponse.ok(msg, Map.of(
                "tokenType", "Bearer",
                "accessToken", access,
                "refreshToken", refresh,
                "expiresIn", jwtService.getAccessExpirationMs() / 1000,
                "userId", user.getId(),
                "username", user.getUsername(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", role,
                "mfaEnabled", user.isMfaEnabled()
        ));
    }

    /* ======================================================================
       REFRESH TOKEN
       ====================================================================== */
    @Transactional
    public ApiResponse<Map<String, Object>> refresh(String refreshToken, HttpServletRequest req) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            return ApiResponse.error("Invalid refresh token");
        }
        if (!JwtService.TYPE_REFRESH.equals(jwtService.extractType(refreshToken))) {
            return ApiResponse.error("Not a refresh token");
        }
        if (blacklistService.isBlacklisted(refreshToken)) {
            return ApiResponse.error("Refresh token revoked");
        }
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElse(null);
        if (stored == null || stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            return ApiResponse.error("Refresh token expired or revoked");
        }
        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String role = user.getRoles().iterator().next().getName().name();

        // Token rotation: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        String newRefresh = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        refreshTokenRepository.save(RefreshToken.builder()
                .token(newRefresh).userId(user.getId())
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .ipAddress(AuditService.clientIp(req))
                .userAgent(req == null ? null : req.getHeader("User-Agent"))
                .build());
        String newAccess = jwtService.generateAccessToken(user.getId(), user.getUsername(), role);

        sessionService.touch(refreshToken);

        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_TOKEN_REFRESH, "INFO", "Token rotated", req);
        return ApiResponse.ok("Token refreshed", Map.of(
                "tokenType", "Bearer",
                "accessToken", newAccess,
                "refreshToken", newRefresh,
                "expiresIn", jwtService.getAccessExpirationMs() / 1000
        ));
    }

    /* ======================================================================
       LOGOUT (single device)  /  LOGOUT ALL
       ====================================================================== */
    @Transactional
    public ApiResponse<String> logout(String accessToken, String refreshToken, Long userId, HttpServletRequest req) {
        if (accessToken != null)  blacklistService.blacklist(accessToken, userId, "logout");
        if (refreshToken != null) {
            sessionService.terminateSession(refreshToken);
        }
        auditService.log(userId, null, AuditService.ACTION_LOGOUT, "Info", "User logged out", req);
        return ApiResponse.ok("Logged out successfully", "OK");
    }

    @Transactional
    public ApiResponse<String> logoutAll(Long userId, HttpServletRequest req) {
        sessionService.terminateAllForUser(userId);
        auditService.log(userId, null, AuditService.ACTION_LOGOUT_ALL, "WARN", "All sessions terminated", req);
        return ApiResponse.ok("All sessions terminated", "OK");
    }

    /* ======================================================================
       CHANGE PASSWORD  (authenticated, requires current pwd)
       ====================================================================== */
    @Transactional
    public ApiResponse<String> changePassword(Long userId, String currentPwd, String newPwd, HttpServletRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPwd, user.getPassword())) {
            auditService.log(userId, user.getUsername(), AuditService.ACTION_PASSWORD_CHANGE, "WARN", "Bad current pwd", req);
            return ApiResponse.error("Current password is incorrect");
        }
        if (newPwd.length() < 6) return ApiResponse.error("New password must be at least 6 characters");
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
        // Invalidate all other sessions for safety
        sessionService.terminateAllForUser(userId);
        // Keep the current session? No - safer to log out all on password change.
        auditService.log(userId, user.getUsername(), AuditService.ACTION_PASSWORD_CHANGE, "INFO", "Password changed", req);
        return ApiResponse.ok("Password changed. Please log in again.", "OK");
    }

    /* ======================================================================
       PASSWORD RESET  (forgot flow)
       ====================================================================== */
    @Transactional
    public ApiResponse<String> requestPasswordReset(String emailOrUsername, HttpServletRequest req) {
        Optional<User> opt = userRepository.findByEmail(emailOrUsername)
                .or(() -> userRepository.findByUsername(emailOrUsername));
        // Always return the same message to avoid user enumeration
        if (opt.isEmpty()) return ApiResponse.ok("If an account exists, a reset link has been sent.", "OK");
        User user = opt.get();
        String token = generateSecureToken(48);
        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(60L * 30))
                .used(false)
                .build();
        passwordResetTokenRepository.save(prt);
        emailService.sendPasswordReset(user.getEmail(), resetLinkBase + token);
        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_PASSWORD_RESET_REQ, "INFO", "Reset email sent", req);
        return ApiResponse.ok("If an account exists, a reset link has been sent.", "OK");
    }

    @Transactional
    public ApiResponse<String> confirmPasswordReset(String token, String newPassword, HttpServletRequest req) {
        if (newPassword == null || newPassword.length() < 6) return ApiResponse.error("Password must be at least 6 characters");
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token).orElse(null);
        if (prt == null || prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            return ApiResponse.error("Invalid or expired reset token");
        }
        User user = userRepository.findById(prt.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockTime(null);
        userRepository.save(user);
        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
        sessionService.terminateAllForUser(user.getId());
        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_PASSWORD_RESET_DONE, "WARN", "Password reset complete", req);
        return ApiResponse.ok("Password reset successful. Please log in.", "OK");
    }

    /* ======================================================================
       MFA enable / disable
       ====================================================================== */
    @Transactional
    public ApiResponse<Map<String, Object>> enableMfa(Long userId, String channel, HttpServletRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String ch = (channel == null || channel.isBlank()) ? user.getMfaChannel() : channel.toUpperCase();
        if (!ch.equals("EMAIL") && !ch.equals("SMS")) return ApiResponse.error("Invalid channel");
        user.setMfaChannel(ch);
        String code = otpService.issueOtp(user.getId(), user.getEmail(), OtpService.PURPOSE_ENABLE_MFA, ch);
        return ApiResponse.ok("OTP sent to verify MFA setup", Map.of("channel", ch, "devCode", code));
    }

    @Transactional
    public ApiResponse<String> confirmEnableMfa(Long userId, String code, HttpServletRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!otpService.verifyOtp(user.getId(), code, OtpService.PURPOSE_ENABLE_MFA)) {
            return ApiResponse.error("Invalid or expired verification code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_MFA_ENABLED, "INFO", "MFA enabled (" + user.getMfaChannel() + ")", req);
        return ApiResponse.ok("MFA enabled successfully", "OK");
    }

    @Transactional
    public ApiResponse<String> disableMfa(Long userId, String currentPwd, HttpServletRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPwd, user.getPassword())) return ApiResponse.error("Password is incorrect");
        user.setMfaEnabled(false);
        userRepository.save(user);
        auditService.log(user.getId(), user.getUsername(), AuditService.ACTION_MFA_DISABLED, "WARN", "MFA disabled", req);
        return ApiResponse.ok("MFA disabled", "OK");
    }

    /* ======================================================================
       SESSION / HISTORY  (read-only views for the user)
       ====================================================================== */
    public List<LoginSession> activeSessions(Long userId) {
        return sessionService.activeSessions(userId);
    }

    public List<LoginHistory> loginHistory(Long userId) {
        return loginHistoryRepository.findTop20ByUserIdOrderByLoggedInAtDesc(userId);
    }

    @Transactional
    public ApiResponse<String> terminateSession(Long userId, String refreshToken, HttpServletRequest req) {
        sessionService.terminateSession(refreshToken);
        auditService.log(userId, null, AuditService.ACTION_LOGOUT, "Info", "Session terminated by user", req);
        return ApiResponse.ok("Session terminated", "OK");
    }

    /* ======================================================================
       Helpers
       ====================================================================== */
    private void recordHistory(Long userId, String username, String status, HttpServletRequest req, String reason) {
        try {
            LoginHistory h = LoginHistory.builder()
                    .userId(userId)
                    .username(username)
                    .status(status)
                    .ipAddress(AuditService.clientIp(req))
                    .userAgent(req == null ? null : req.getHeader("User-Agent"))
                    .deviceId(DeviceParser.fingerprint(req == null ? null : req.getHeader("User-Agent")))
                    .failureReason(reason)
                    .loggedInAt(java.time.Instant.now())
                    .build();
            loginHistoryRepository.save(h);
        } catch (Exception ignored) { /* never break login on logging errors */ }
    }

    private String generateSecureToken(int bytes) {
        byte[] buf = new byte[bytes];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
