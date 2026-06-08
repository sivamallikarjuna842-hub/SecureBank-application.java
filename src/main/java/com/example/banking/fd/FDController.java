package com.example.banking.fd;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fds")
@RequiredArgsConstructor
public class FDController {

    private final FixedDepositService fdService;
    private final UserUtil userUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<FixedDeposit>> createFD(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam String fromAccount,
            @RequestParam BigDecimal amount,
            @RequestParam int tenureMonths) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(fdService.createFD(userId, fromAccount, amount, tenureMonths));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedDeposit>>> getMyFDs(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(fdService.getUserFDs(userId));
    }

    @GetMapping("/{fdNumber}")
    public ResponseEntity<ApiResponse<FixedDeposit>> getFD(@PathVariable String fdNumber) {
        return ResponseEntity.ok(fdService.getFD(fdNumber));
    }

    @PostMapping("/{id}/premature-withdraw")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prematureWithdraw(@PathVariable Long id) {
        return ResponseEntity.ok(fdService.prematureWithdraw(id));
    }
}