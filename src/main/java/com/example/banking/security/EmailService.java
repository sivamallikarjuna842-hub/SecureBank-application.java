package com.example.banking.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * EmailService - production-ready wrapper around JavaMailSender.
 * If no SMTP is configured it falls back to logging (dev mode).
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async
    public void sendOtp(String to, String code, String purpose) {
        String subject = "[SecureBank] Your verification code";
        String body = "Your SecureBank verification code is: " + code + "\n\n"
                + "Purpose: " + purpose + "\n"
                + "Issued at: " + LocalDateTime.now().format(TS) + "\n"
                + "This code expires in 5 minutes.\n\n"
                + "If you did not request this, please secure your account immediately.";
        send(to, subject, body);
    }

    @Async
    public void sendPasswordReset(String to, String resetLink) {
        String subject = "[SecureBank] Password reset request";
        String body = "We received a request to reset your SecureBank password.\n\n"
                + "Click the link below (or paste it in your browser) within 30 minutes:\n\n"
                + resetLink + "\n\n"
                + "If you did not request a password reset, please ignore this email.";
        send(to, subject, body);
    }

    @Async
    public void sendLoginAlert(String to, String ip, String device, String when) {
        String subject = "[SecureBank] New login to your account";
        String body = "A new login to your SecureBank account was detected.\n\n"
                + "Time:  " + when + "\n"
                + "IP:    " + ip + "\n"
                + "Device:" + device + "\n\n"
                + "If this was not you, please change your password and contact support.";
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        if (emailEnabled && mailSender != null && fromAddress != null && !fromAddress.isBlank()) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(fromAddress);
                msg.setTo(to);
                msg.setSubject(subject);
                msg.setText(body);
                mailSender.send(msg);
                log.info("Email sent to {} subject='{}'", to, subject);
                return;
            } catch (Exception e) {
                log.warn("Failed to send email, falling back to log: {}", e.getMessage());
            }
        }
        // Dev fallback
        log.info("==== [DEV EMAIL] To: {} | Subject: {} | Body: {}", to, subject, body.replace("\n", " | "));
    }
}
