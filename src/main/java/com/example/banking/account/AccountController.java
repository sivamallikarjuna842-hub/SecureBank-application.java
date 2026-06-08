package com.example.banking.account;

import com.example.banking.common.AccountStatus;
import com.example.banking.common.AccountType;
import com.example.banking.common.ApiResponse;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserUtil userUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam AccountType type) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(accountService.createAccount(userId, type));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getMyAccounts(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PutMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<Account>> updateStatus(
            @PathVariable String accountNumber,
            @RequestParam AccountStatus status) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, status));
    }
}