package com.example.banking.common;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ReferenceNumberGenerator {
    private static final AtomicLong counter = new AtomicLong(1);

    public String generateAccountNumber() {
        return "ACC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
               + String.format("%04d", counter.getAndIncrement() % 10000);
    }

    public String generateTransactionReference() {
        return "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
               + String.format("%04d", counter.getAndIncrement() % 10000);
    }

    public String generateLoanApplicationNumber() {
        return "LOAN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
               + String.format("%04d", counter.getAndIncrement() % 10000);
    }

    public String generateFDNumber() {
        return "FD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
               + String.format("%04d", counter.getAndIncrement() % 10000);
    }

    public String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    public String generateCvv() {
        return String.format("%03d", (int) (Math.random() * 1000));
    }

    public String generateTicketNumber() {
        return "TKT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
               + String.format("%04d", counter.getAndIncrement() % 10000);
    }
}