package com.example.banking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    public static final String PURPOSE_LOGIN          = "LOGIN";
    public static final String PURPOSE_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String PURPOSE_CHANGE_PASS    = "CHANGE_PASSWORD";
    public static final String PURPOSE_ENABLE_MFA     = "ENABLE_MFA";
    public static final String CHANNEL_EMAIL          = "EMAIL";
    public static final String CHANNEL_SMS            = "SMS";

    private static final SecureRandom RNG = new SecureRandom();
    private static final long TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Issue a fresh 6-digit OTP, replacing any unconsumed prior one. */
    @Transactional
    public String issueOtp(Long userId, String contact, String purpose, String channel) {
        // Wipe any prior unconsumed tokens for this user+purpose
        otpTokenRepository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(userId, purpose)
                .ifPresent(otpTokenRepository::delete);

        String code = String.format("%06d", RNG.nextInt(1_000_000));
        OtpToken token = OtpToken.builder()
                .userId(userId)
                .codeHash(passwordEncoder.encode(code))
                .purpose(purpose)
                .channel(channel)
                .expiresAt(Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES))
                .consumed(false)
                .attempts(0)
                .build();
        otpTokenRepository.save(token);

        if (CHANNEL_EMAIL.equalsIgnoreCase(channel)) {
            emailService.sendOtp(contact, code, purpose);
        } else {
            // SMS not configured in dev - log it
            emailService.sendOtp(contact, code, "[SMS-STUB] " + purpose);
        }
        return code; // returned in dev for testing; not exposed via API
    }

    /** Verify an OTP. Returns true on success; throws on failure (and increments attempts). */
    @Transactional
    public boolean verifyOtp(Long userId, String code, String purpose) {
        Optional<OtpToken> opt = otpTokenRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(userId, purpose);
        if (opt.isEmpty()) return false;
        OtpToken token = opt.get();
        if (token.isConsumed() || token.getExpiresAt().isBefore(Instant.now())) return false;
        if (token.getAttempts() >= MAX_ATTEMPTS) return false;

        token.setAttempts(token.getAttempts() + 1);
        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            otpTokenRepository.save(token);
            return false;
        }
        token.setConsumed(true);
        otpTokenRepository.save(token);
        return true;
    }
}
