package com.example.banking.account;

import com.example.banking.common.*;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator refGen;

    @Value("${app.account.savings-min-balance:500}")
    private BigDecimal savingsMinBalance;

    @Value("${app.account.current-min-balance:1000}")
    private BigDecimal currentMinBalance;

    public ApiResponse<Account> createAccount(Long userId, AccountType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = Account.builder()
                .accountNumber(refGen.generateAccountNumber())
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .minBalance(type == AccountType.SAVINGS ? savingsMinBalance : currentMinBalance)
                .status(AccountStatus.ACTIVE)
                .user(user)
                .build();

        accountRepository.save(account);
        return ApiResponse.ok("Account created successfully", account);
    }

    public ApiResponse<List<Account>> getUserAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(accountRepository.findByUser(user));
    }

    public ApiResponse<Account> getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ApiResponse.ok(account);
    }

    public ApiResponse<Account> updateAccountStatus(String accountNumber, AccountStatus status) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus(status);
        accountRepository.save(account);
        return ApiResponse.ok("Account status updated", account);
    }

    public ApiResponse<Map<String, Object>> getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return ApiResponse.ok(Map.of(
                "accountNumber", account.getAccountNumber(),
                "balance", account.getBalance(),
                "status", account.getStatus()
        ));
    }

    public List<Account> getAccountsByUser(User user) {
        return accountRepository.findByUser(user);
    }
}