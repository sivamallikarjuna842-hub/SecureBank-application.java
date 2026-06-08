package com.example.banking.loan;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.LoanStatus;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final UserUtil userUtil;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<Loan>> applyForLoan(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam BigDecimal amount,
            @RequestParam int tenureMonths,
            @RequestParam(required = false) String purpose) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(loanService.applyForLoan(userId, amount, tenureMonths, purpose));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Loan>>> getMyLoans(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(loanService.getUserLoans(userId));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Loan>> approveLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(loanService.approveLoan(id, remarks));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Loan>> activateLoan(
            @PathVariable Long id,
            @RequestParam String accountNumber) {
        return ResponseEntity.ok(loanService.activateLoan(id, accountNumber));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Loan>> rejectLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(loanService.rejectLoan(id, reason));
    }

    @PostMapping("/{id}/repay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> repayLoan(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(loanService.repayLoan(id, amount));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Loan>>> getPendingLoans() {
        return ResponseEntity.ok(loanService.getPendingLoans());
    }

    @GetMapping("/predict")
    public ResponseEntity<ApiResponse<Map<String, Object>>> predictEligibility(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam BigDecimal amount) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(loanService.predictLoanEligibility(userId, amount));
    }

    @PostMapping("/chatbot")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chatbot(
            @RequestParam String query) {
        return ResponseEntity.ok(loanService.chatbotQuery(query));
    }
}