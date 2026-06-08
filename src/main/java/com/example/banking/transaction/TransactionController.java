package com.example.banking.transaction;

import com.example.banking.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Transaction>> deposit(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(transactionService.deposit(accountNumber, amount, description));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Transaction>> withdraw(
            @RequestParam String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(transactionService.withdraw(accountNumber, amount, description));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transfer(
            @RequestParam String fromAccount,
            @RequestParam String toAccount,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(transactionService.transfer(fromAccount, toAccount, amount, description));
    }

    @PostMapping("/transfer-beneficiary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transferToBeneficiary(
            @RequestParam String fromAccount,
            @RequestParam Long beneficiaryId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(transactionService.transferToBeneficiary(fromAccount, beneficiaryId, amount, description));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistory(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }

    @GetMapping("/{accountNumber}/mini-statement")
    public ResponseEntity<ApiResponse<List<Transaction>>> getMiniStatement(@PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getMiniStatement(accountNumber));
    }

    @GetMapping("/{accountNumber}/by-date")
    public ResponseEntity<ApiResponse<List<Transaction>>> getHistoryByDate(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(transactionService.getTransactionHistoryByDateRange(accountNumber, fromDate, toDate));
    }
}