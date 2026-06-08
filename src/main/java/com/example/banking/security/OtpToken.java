package com.example.banking.security;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "otp_tokens", indexes = {
    @Index(name = "idx_otp_user_purpose", columnList = "user_id,purpose")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 6-digit numeric code, hashed in storage */
    @Column(nullable = false, length = 128)
    private String codeHash;

    /** LOGIN, RESET_PASSWORD, CHANGE_PASSWORD, ENABLE_MFA */
    @Column(nullable = false, length = 32)
    private String purpose;

    /** EMAIL or SMS */
    @Column(nullable = false, length = 16)
    private String channel;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(nullable = false)
    private int attempts;

    private Instant createdAt;

    @PrePersist
    protected void onCreate() { createdAt = Instant.now(); }
}
