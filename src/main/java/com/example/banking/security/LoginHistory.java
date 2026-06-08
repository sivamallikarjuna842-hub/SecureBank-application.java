package com.example.banking.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_login_history_user", columnList = "user_id,logged_in_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(nullable = false, length = 16)
    private String status; // SUCCESS, FAILURE, LOCKED, MFA_REQUIRED, BLOCKED

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 256)
    private String location;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 64)
    private String deviceId;

    @Column(length = 256)
    private String failureReason;

    @Column(name = "logged_in_at", nullable = false)
    private Instant loggedInAt;
}
