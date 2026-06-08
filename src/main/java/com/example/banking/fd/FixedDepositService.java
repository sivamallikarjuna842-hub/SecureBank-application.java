package com.example.banking.fd;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.common.ApiResponse;
import com.example.banking.common.ReferenceNumberGenerator;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FixedDepositService {

    private final FixedDepositRepository fdRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator refGen;

    @Value("${app.fd.min-amount:10000}")
    private BigDecimal minFdAmount;

    @Value("${app.fd.interest-rate-1yr:6.5}")
    private BigDecimal rate1yr;

    @Value("${app.fd.interest-rate-3yr:7.0}")
    private BigDecimal rate3yr;

    @Value("${app.fd.interest-rate-5yr:7.5}")
    private BigDecimal rate5yr;

    public ApiResponse<FixedDeposit> createFD(Long userId, String fromAccountNumber,
                                              BigDecimal amount, int tenureMonths) {
        if (amount.compareTo(minFdAmount) < 0) {
            return ApiResponse.error("Minimum FD amount is " + minFdAmount);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            return ApiResponse.error("Account does not belong to this user");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            return ApiResponse.error("Insufficient balance");
        }

        // Deduct amount from account
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        BigDecimal interestRate = getInterestRate(tenureMonths);
        BigDecimal maturityAmount = calculateMaturityAmount(amount, interestRate, tenureMonths);
        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(tenureMonths);

        FixedDeposit fd = FixedDeposit.builder()
                .fdNumber(refGen.generateFDNumber())
                .amount(amount)
                .interestRate(interestRate)
                .tenureMonths(tenureMonths)
                .startDate(startDate)
                .maturityDate(maturityDate)
                .maturityAmount(maturityAmount)
                .user(user)
                .account(account)
                .build();

        fdRepository.save(fd);
        return ApiResponse.ok("Fixed Deposit created successfully", fd);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> prematureWithdraw(Long fdId) {
        FixedDeposit fd = fdRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

        if (fd.isClosed()) {
            return ApiResponse.error("FD is already closed");
        }
        if (fd.isMatured()) {
            return ApiResponse.error("FD has matured. Use normal withdrawal.");
        }

        long daysHeld = ChronoUnit.DAYS.between(fd.getStartDate(), LocalDate.now());
        BigDecimal penaltyRate = BigDecimal.valueOf(1.0); // 1% penalty
        BigDecimal applicableRate = fd.getInterestRate().subtract(penaltyRate);
        if (applicableRate.compareTo(BigDecimal.ZERO) < 0) {
            applicableRate = BigDecimal.ZERO;
        }

        // Simple interest for premature withdrawal
        BigDecimal interest = fd.getAmount()
                .multiply(applicableRate)
                .multiply(BigDecimal.valueOf(daysHeld))
                .divide(BigDecimal.valueOf(365 * 100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = fd.getAmount().add(interest);

        // Credit back to account
        Account account = fd.getAccount();
        account.setBalance(account.getBalance().add(totalAmount));
        accountRepository.save(account);

        fd.setClosed(true);
        fd.setMatured(true);
        fdRepository.save(fd);

        return ApiResponse.ok("Premature withdrawal successful. Penalty applied.", Map.of(
                "fdNumber", fd.getFdNumber(),
                "principal", fd.getAmount(),
                "interestEarned", interest,
                "totalAmount", totalAmount,
                "penaltyRate", penaltyRate + "%"
        ));
    }

    public ApiResponse<List<FixedDeposit>> getUserFDs(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(fdRepository.findByUserOrderByCreatedAtDesc(user));
    }

    public ApiResponse<FixedDeposit> getFD(String fdNumber) {
        List<FixedDeposit> all = fdRepository.findAll();
        return all.stream()
                .filter(fd -> fd.getFdNumber().equals(fdNumber))
                .findFirst()
                .map(fd -> ApiResponse.ok(fd))
                .orElse(ApiResponse.error("FD not found"));
    }

    private BigDecimal getInterestRate(int tenureMonths) {
        if (tenureMonths <= 12) return rate1yr;
        if (tenureMonths <= 36) return rate3yr;
        return rate5yr;
    }

    private BigDecimal calculateMaturityAmount(BigDecimal principal, BigDecimal rate, int months) {
        BigDecimal years = BigDecimal.valueOf(months).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal interest = principal.multiply(rate).multiply(years)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return principal.add(interest);
    }
}