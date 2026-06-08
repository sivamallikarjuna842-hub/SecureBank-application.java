package com.example.banking.common;

import com.example.banking.security.JwtService;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUtil {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    public Long getCurrentUserId(String username) {
        return getCurrentUser(username).getId();
    }

    /* ====== Token-based convenience helpers ====== */

    public Long currentUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return null;
        try { return jwtService.extractUserId(accessToken); }
        catch (Exception e) { return null; }
    }

    public String currentUsername(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return null;
        try { return jwtService.extractUsername(accessToken); }
        catch (Exception e) { return null; }
    }

    public String currentRole(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return null;
        try { return jwtService.extractRole(accessToken); }
        catch (Exception e) { return null; }
    }
}
