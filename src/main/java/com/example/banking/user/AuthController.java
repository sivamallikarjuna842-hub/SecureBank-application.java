package com.example.banking.user;

import com.example.banking.common.ApiResponse;
import com.example.banking.security.JwtService;
import com.example.banking.security.LoginHistory;
import com.example.banking.security.LoginSession;
import com.example.banking.common.UserUtil;
import com.example.banking.security.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserUtil userUtil;

    /* -------------------- PUBLIC -------------------- */

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.register(fullName, email, username, password, req));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String deviceId,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.login(username, password, deviceId, req));
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyLoginOtp(
            @RequestParam String username,
            @RequestParam String code,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.verifyLoginOtp(username, code, req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(
            @RequestParam String refreshToken,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.refresh(refreshToken, req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestParam String emailOrUsername,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.requestPasswordReset(emailOrUsername, req));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            HttpServletRequest req) {
        return ResponseEntity.ok(authService.confirmPasswordReset(token, newPassword, req));
    }

    /* -------------------- AUTHENTICATED -------------------- */

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest req) {
        String access  = extractAccessToken(req);
        String refresh = req.getParameter("refreshToken");
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.logout(access, refresh, uid, req));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<String>> logoutAll(HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.logoutAll(uid, req));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.changePassword(uid, currentPassword, newPassword, req));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<LoginSession>>> mySessions(HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(ApiResponse.ok("Active sessions", authService.activeSessions(uid)));
    }

    @DeleteMapping("/sessions/{refreshToken}")
    public ResponseEntity<ApiResponse<String>> terminateSession(
            @PathVariable String refreshToken, HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.terminateSession(uid, refreshToken, req));
    }

    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse<List<LoginHistory>>> loginHistory(HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(ApiResponse.ok("Login history", authService.loginHistory(uid)));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enableMfa(
            @RequestParam(required = false, defaultValue = "EMAIL") String channel,
            HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.enableMfa(uid, channel, req));
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<ApiResponse<String>> confirmMfa(
            @RequestParam String code,
            HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.confirmEnableMfa(uid, code, req));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<ApiResponse<String>> disableMfa(
            @RequestParam String currentPassword,
            HttpServletRequest req) {
        String access = extractAccessToken(req);
        Long uid = userUtil.currentUserId(access);
        return ResponseEntity.ok(authService.disableMfa(uid, currentPassword, req));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(HttpServletRequest req) {
        String access = extractAccessToken(req);
        return ResponseEntity.ok(ApiResponse.ok("Current user", Map.of(
                "userId",  userUtil.currentUserId(access),
                "username", userUtil.currentUsername(access),
                "role",     userUtil.currentRole(access)
        )));
    }

    /* -------------------- HEALTH / PING -------------------- */

    /** Lightweight unauthenticated liveness check. */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("pong", Map.of(
                "server", "SecureBank API",
                "version", "1.0.0",
                "time",   System.currentTimeMillis(),
                "client", AuditService.clientIp(req)
        )));
    }

    /** Authenticated readiness check (validates the token). */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health(HttpServletRequest req) {
        String access = extractAccessToken(req);
        return ResponseEntity.ok(ApiResponse.ok("healthy", Map.of(
                "status",   "UP",
                "userId",   userUtil.currentUserId(access),
                "username", userUtil.currentUsername(access),
                "role",     userUtil.currentRole(access),
                "uptime",   System.currentTimeMillis()
        )));
    }

    /* -------------------- helper -------------------- */
    private String extractAccessToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null;
    }
}
