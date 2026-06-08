package com.example.banking.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_time", columnList = "user_id,created_at"),
    @Index(name = "idx_audit_action",    columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String action; // LOGIN_SUCCESS, LOGIN_FAIL, LOGOUT, TOKEN_REFRESH, PASSWORD_CHANGE, etc.

    @Column(length = 32)
    private String severity; // INFO, WARN, CRITICAL

    @Column(length = 256)
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 64)
    private String deviceId;

    @Column(length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
