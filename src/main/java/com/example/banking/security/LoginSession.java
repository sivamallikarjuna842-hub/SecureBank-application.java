package com.example.banking.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "login_sessions", indexes = {
    @Index(name = "idx_session_user", columnList = "user_id"),
    @Index(name = "idx_session_token", columnList = "refresh_token", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(length = 64)
    private String deviceId;

    @Column(length = 256)
    private String deviceName;

    @Column(length = 64)
    private String os;

    @Column(length = 64)
    private String browser;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 256)
    private String location;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant lastActiveAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active;
}
