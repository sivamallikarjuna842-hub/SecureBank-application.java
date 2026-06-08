package com.example.banking.admin;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.common.*;
import com.example.banking.loan.Loan;
import com.example.banking.loan.LoanRepository;
import com.example.banking.support.SupportTicket;
import com.example.banking.support.SupportTicketRepository;
import com.example.banking.transaction.Transaction;
import com.example.banking.transaction.TransactionRepository;
import com.example.banking.user.Role;
import com.example.banking.user.RoleRepository;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final SupportTicketRepository ticketRepository;

    public ApiResponse<List<User>> getAllCustomers() {
        Role customerRole = roleRepository.findByName(RoleType.ROLE_CUSTOMER).orElse(null);
        if (customerRole == null) return ApiResponse.ok(List.of());
        return ApiResponse.ok(userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(customerRole))
                .toList());
    }

    public ApiResponse<List<Account>> getAllAccounts() {
        return ApiResponse.ok(accountRepository.findAll());
    }

    public ApiResponse<List<Transaction>> getFlaggedTransactions() {
        return ApiResponse.ok(transactionRepository.findByFlaggedTrue());
    }

    public ApiResponse<User> freezeUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(false);
        userRepository.save(user);

        List<Account> accounts = accountRepository.findByUser(user);
        accounts.forEach(a -> a.setStatus(AccountStatus.FROZEN));
        accountRepository.saveAll(accounts);

        return ApiResponse.ok("Account frozen", user);
    }

    public ApiResponse<User> unfreezeUserAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        List<Account> accounts = accountRepository.findByUser(user);
        accounts.forEach(a -> a.setStatus(AccountStatus.ACTIVE));
        accountRepository.saveAll(accounts);

        return ApiResponse.ok("Account unfrozen", user);
    }

    public ApiResponse<Map<String, Object>> getDashboardSummary() {
        long totalUsers = userRepository.count();
        long totalAccounts = accountRepository.count();
        long totalLoans = loanRepository.count();
        long pendingLoans = loanRepository.findByStatusOrderByAppliedAtAsc(LoanStatus.PENDING).size();
        long flaggedTxns = transactionRepository.findByFlaggedTrue().size();
        long openTickets = ticketRepository.findByStatusOrderByCreatedAtDesc("OPEN").size();

        return ApiResponse.ok(Map.of(
                "totalUsers", totalUsers,
                "totalAccounts", totalAccounts,
                "totalLoans", totalLoans,
                "pendingLoans", pendingLoans,
                "flaggedTransactions", flaggedTxns,
                "openTickets", openTickets
        ));
    }

    // Reporting
    public ApiResponse<Map<String, Object>> getDailyTransactionReport(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        List<Transaction> allTxns = transactionRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(from) && !t.getTransactionDate().isAfter(to))
                .toList();

        double totalDeposits = allTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT)
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double totalWithdrawals = allTxns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL ||
                             t.getTransactionType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        return ApiResponse.ok(Map.of(
                "date", date.toString(),
                "totalTransactions", allTxns.size(),
                "totalDeposits", totalDeposits,
                "totalWithdrawals", totalWithdrawals,
                "netFlow", totalDeposits - totalWithdrawals
        ));
    }

    public ApiResponse<Map<String, Object>> getMonthlyStatement(Long accountId, int year, int month) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        LocalDateTime from = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1).minusSeconds(1);

        List<Transaction> txns = transactionRepository
                .findByAccountAndTransactionDateBetweenOrderByTransactionDateDesc(account, from, to);

        double totalCredits = txns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT ||
                             t.getTransactionType() == TransactionType.TRANSFER_IN)
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();
        double totalDebits = txns.stream()
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAWAL ||
                             t.getTransactionType() == TransactionType.TRANSFER_OUT)
                .mapToDouble(t -> t.getAmount().doubleValue()).sum();

        return ApiResponse.ok(Map.of(
                "accountNumber", account.getAccountNumber(),
                "month", month,
                "year", year,
                "transactions", txns,
                "totalCredits", totalCredits,
                "totalDebits", totalDebits,
                "closingBalance", account.getBalance()
        ));
    }

    public ApiResponse<List<Loan>> getLoanReport(LoanStatus status) {
        return ApiResponse.ok(loanRepository.findByStatusOrderByAppliedAtAsc(status));
    }
}