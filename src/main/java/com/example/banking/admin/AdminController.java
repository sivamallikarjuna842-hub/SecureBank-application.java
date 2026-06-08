package com.example.banking.admin;

import com.example.banking.account.Account;
import com.example.banking.common.ApiResponse;
import com.example.banking.common.LoanStatus;
import com.example.banking.loan.Loan;
import com.example.banking.transaction.Transaction;
import com.example.banking.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(adminService.getDashboardSummary());
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<User>>> getAllCustomers() {
        return ResponseEntity.ok(adminService.getAllCustomers());
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<Account>>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @GetMapping("/flagged-transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getFlaggedTransactions() {
        return ResponseEntity.ok(adminService.getFlaggedTransactions());
    }

    @PutMapping("/users/{userId}/freeze")
    public ResponseEntity<ApiResponse<User>> freezeUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.freezeUserAccount(userId));
    }

    @PutMapping("/users/{userId}/unfreeze")
    public ResponseEntity<ApiResponse<User>> unfreezeUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.unfreezeUserAccount(userId));
    }

    @GetMapping("/reports/daily-transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dailyTransactionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(adminService.getDailyTransactionReport(date));
    }

    @GetMapping("/reports/monthly-statement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> monthlyStatement(
            @RequestParam Long accountId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(adminService.getMonthlyStatement(accountId, year, month));
    }

    @GetMapping("/reports/loans")
    public ResponseEntity<ApiResponse<List<Loan>>> loanReport(
            @RequestParam(required = false) LoanStatus status) {
        if (status == null) status = LoanStatus.PENDING;
        return ResponseEntity.ok(adminService.getLoanReport(status));
    }
}