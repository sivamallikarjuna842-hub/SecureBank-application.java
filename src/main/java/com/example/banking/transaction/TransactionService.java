package com.example.banking.transaction;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.beneficiary.Beneficiary;
import com.example.banking.beneficiary.BeneficiaryRepository;
import com.example.banking.common.*;
import com.example.banking.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ReferenceNumberGenerator refGen;

    @Value("${app.fraud.max-daily-transactions:10}")
    private int maxDailyTransactions;

    @Value("${app.fraud.max-daily-amount:50000}")
    private BigDecimal maxDailyAmount;

    public ApiResponse<Transaction> deposit(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Amount must be positive");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            return ApiResponse.error("Account is not active");
        }

        BigDecimal before = account.getBalance();
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .description(description != null ? description : "Deposit")
                .account(account)
                .build();

        transactionRepository.save(txn);
        return ApiResponse.ok("Deposit successful", txn);
    }

    public ApiResponse<Transaction> withdraw(String accountNumber, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Amount must be positive");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            return ApiResponse.error("Account is not active");
        }

        BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(amount);
        if (balanceAfterWithdrawal.compareTo(account.getMinBalance()) < 0) {
            return ApiResponse.error("Insufficient balance. Minimum balance required: " + account.getMinBalance());
        }

        BigDecimal before = account.getBalance();
        account.setBalance(balanceAfterWithdrawal);
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(before)
                .balanceAfter(account.getBalance())
                .description(description != null ? description : "Withdrawal")
                .account(account)
                .build();

        transactionRepository.save(txn);
        return ApiResponse.ok("Withdrawal successful", txn);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> transfer(String fromAccountNumber, String toAccountNumber,
                                                      BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Amount must be positive");
        }

        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("Destination account not found"));

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            return ApiResponse.error("Source account is not active");
        }
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            return ApiResponse.error("Destination account is not active");
        }

        // Fraud check
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        Double dailySum = transactionRepository.sumOutgoingSince(fromAccount, since);
        Long dailyCount = transactionRepository.countOutgoingSince(fromAccount, since);

        boolean flagged = false;
        String flagReason = null;
        if (dailyCount != null && dailyCount >= maxDailyTransactions) {
            flagged = true;
            flagReason = "Exceeded max daily transaction count";
        }
        if (dailySum != null && BigDecimal.valueOf(dailySum).add(amount).compareTo(maxDailyAmount) > 0) {
            flagged = true;
            flagReason = "Exceeded max daily transaction amount";
        }

        BigDecimal balanceAfterTransfer = fromAccount.getBalance().subtract(amount);
        if (balanceAfterTransfer.compareTo(fromAccount.getMinBalance()) < 0) {
            return ApiResponse.error("Insufficient balance in source account");
        }

        // Withdraw from source
        BigDecimal fromBefore = fromAccount.getBalance();
        fromAccount.setBalance(balanceAfterTransfer);
        accountRepository.save(fromAccount);

        Transaction txnOut = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.TRANSFER_OUT)
                .amount(amount)
                .balanceBefore(fromBefore)
                .balanceAfter(fromAccount.getBalance())
                .description(description != null ? description : "Transfer to " + toAccountNumber)
                .account(fromAccount)
                .flagged(flagged)
                .flagReason(flagReason)
                .build();

        // Deposit to destination
        BigDecimal toBefore = toAccount.getBalance();
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        Transaction txnIn = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.TRANSFER_IN)
                .amount(amount)
                .balanceBefore(toBefore)
                .balanceAfter(toAccount.getBalance())
                .description(description != null ? description : "Transfer from " + fromAccountNumber)
                .account(toAccount)
                .build();

        transactionRepository.save(txnOut);
        transactionRepository.save(txnIn);

        return ApiResponse.ok("Transfer successful", Map.of(
                "transactionReference", txnOut.getTransactionReference(),
                "fromAccount", fromAccountNumber,
                "toAccount", toAccountNumber,
                "amount", amount,
                "flagged", flagged,
                "flagReason", flagReason != null ? flagReason : "None"
        ));
    }

    @Transactional
    public ApiResponse<Map<String, Object>> transferToBeneficiary(String fromAccountNumber, Long beneficiaryId,
                                                                    BigDecimal amount, String description) {
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("Source account not found"));

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        if (!beneficiary.isVerified()) {
            return ApiResponse.error("Beneficiary is not verified. Please verify first.");
        }

        if (!beneficiary.getUser().getId().equals(fromAccount.getUser().getId())) {
            return ApiResponse.error("Beneficiary does not belong to this user");
        }

        return transfer(fromAccountNumber, beneficiary.getAccountNumber(), amount,
                description != null ? description : "Transfer to beneficiary " + beneficiary.getName());
    }

    public ApiResponse<List<Transaction>> getTransactionHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ApiResponse.ok(transactionRepository.findByAccountOrderByTransactionDateDesc(account));
    }

    public ApiResponse<List<Transaction>> getMiniStatement(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        return ApiResponse.ok(
                transactionRepository.findByAccountAndTransactionDateBetweenOrderByTransactionDateDesc(account, from, LocalDateTime.now()));
    }

    public ApiResponse<List<Transaction>> getTransactionHistoryByDateRange(String accountNumber,
                                                                            LocalDate fromDate, LocalDate toDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        return ApiResponse.ok(
                transactionRepository.findByAccountAndTransactionDateBetweenOrderByTransactionDateDesc(account, from, to));
    }
}