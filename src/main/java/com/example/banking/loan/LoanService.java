package com.example.banking.loan;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.common.*;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator refGen;

    @Value("${app.loan.max-amount:1000000}")
    private BigDecimal maxLoanAmount;

    @Value("${app.loan.interest-rate:10.5}")
    private BigDecimal defaultInterestRate;

    @Value("${app.loan.max-tenure-months:60}")
    private int maxTenureMonths;

    public ApiResponse<Loan> applyForLoan(Long userId, BigDecimal amount, int tenureMonths,
                                           String purpose) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Loan amount must be positive");
        }
        if (amount.compareTo(maxLoanAmount) > 0) {
            return ApiResponse.error("Loan amount exceeds maximum limit of " + maxLoanAmount);
        }
        if (tenureMonths <= 0 || tenureMonths > maxTenureMonths) {
            return ApiResponse.error("Tenure must be between 1 and " + maxTenureMonths + " months");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal emi = calculateEMI(amount, defaultInterestRate, tenureMonths);

        Loan loan = Loan.builder()
                .loanApplicationNumber(refGen.generateLoanApplicationNumber())
                .amount(amount)
                .interestRate(defaultInterestRate)
                .tenureMonths(tenureMonths)
                .emiAmount(emi)
                .purpose(purpose)
                .status(LoanStatus.PENDING)
                .user(user)
                .build();

        loanRepository.save(loan);
        return ApiResponse.ok("Loan application submitted successfully", loan);
    }

    public ApiResponse<Loan> approveLoan(Long loanId, String adminRemarks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            return ApiResponse.error("Loan is not in pending status");
        }

        loan.setStatus(LoanStatus.APPROVED);
        loan.setAdminRemarks(adminRemarks);
        loan.setDisbursementDate(LocalDate.now());
        loan.setNextEmiDate(LocalDate.now().plusMonths(1));
        loanRepository.save(loan);

        return ApiResponse.ok("Loan approved successfully", loan);
    }

    public ApiResponse<Loan> activateLoan(Long loanId, String accountNumber) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            return ApiResponse.error("Loan must be approved first");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        loan.setStatus(LoanStatus.ACTIVE);
        loan.setAccount(account);
        loanRepository.save(loan);

        // Disburse loan amount to account
        account.setBalance(account.getBalance().add(loan.getAmount()));
        accountRepository.save(account);

        return ApiResponse.ok("Loan activated and amount disbursed", loan);
    }

    public ApiResponse<Loan> rejectLoan(Long loanId, String reason) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        loan.setStatus(LoanStatus.REJECTED);
        loan.setAdminRemarks(reason);
        loanRepository.save(loan);
        return ApiResponse.ok("Loan rejected", loan);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> repayLoan(Long loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            return ApiResponse.error("Loan is not active");
        }

        if (loan.getAccount() == null) {
            return ApiResponse.error("No account linked to this loan");
        }

        BigDecimal newPaid = loan.getAmountPaid().add(amount);
        if (newPaid.compareTo(loan.getAmount()) > 0) {
            return ApiResponse.error("Repayment amount exceeds remaining balance");
        }

        // Deduct from account
        Account account = loan.getAccount();
        if (account.getBalance().compareTo(amount) < 0) {
            return ApiResponse.error("Insufficient balance in linked account");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        loan.setAmountPaid(newPaid);
        if (newPaid.compareTo(loan.getAmount()) >= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        } else {
            loan.setNextEmiDate(LocalDate.now().plusMonths(1));
        }
        loanRepository.save(loan);

        return ApiResponse.ok("Repayment successful", Map.of(
                "loanApplicationNumber", loan.getLoanApplicationNumber(),
                "amountPaid", newPaid,
                "remaining", loan.getAmount().subtract(newPaid),
                "status", loan.getStatus()
        ));
    }

    public ApiResponse<List<Loan>> getUserLoans(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(loanRepository.findByUserOrderByAppliedAtDesc(user));
    }

    public ApiResponse<List<Loan>> getPendingLoans() {
        return ApiResponse.ok(loanRepository.findByStatusOrderByAppliedAtAsc(LoanStatus.PENDING));
    }

    public ApiResponse<List<Loan>> getAllLoansByStatus(LoanStatus status) {
        return ApiResponse.ok(loanRepository.findByStatusOrderByAppliedAtAsc(status));
    }

    private BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12 * 100), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    // AI-powered loan eligibility prediction (simplified rule-based)
    public ApiResponse<Map<String, Object>> predictLoanEligibility(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Account> accounts = accountRepository.findByUser(user);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double score = 0;
        if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
            score = Math.min(100, totalBalance.divide(amount, 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(50)).doubleValue());
        }
        if (accounts.size() >= 2) score += 10;
        if (user.getPhone() != null) score += 10;

        boolean eligible = score >= 40;
        return ApiResponse.ok(Map.of(
                "creditScore", Math.min(900, (int) (score * 9)),
                "eligibilityScore", Math.round(score),
                "eligible", eligible,
                "message", eligible ? "You are likely eligible for this loan" : "Low eligibility score",
                "suggestedAmount", eligible ? amount : totalBalance.multiply(BigDecimal.valueOf(0.5))
        ));
    }

    // AI-powered chatbot response (rules-based)
    public ApiResponse<Map<String, Object>> chatbotQuery(String query) {
        String lower = query.toLowerCase();
        String response;

        if (lower.contains("balance") || lower.contains("how much")) {
            response = "You can check your balance by navigating to Account > Balance Inquiry.";
        } else if (lower.contains("loan") && (lower.contains("eligible") || lower.contains("apply"))) {
            response = "You can apply for a loan through our Loan section. Check your eligibility first!";
        } else if (lower.contains("interest") || lower.contains("rate")) {
            response = "Our current loan interest rate is " + defaultInterestRate + "% per annum.";
        } else if (lower.contains("transfer") || lower.contains("send")) {
            response = "You can transfer funds using the Transfer option in the Transaction menu.";
        } else if (lower.contains("fd") || lower.contains("fixed deposit")) {
            response = "Fixed Deposits start from ₹10,000 with interest rates up to 7.5%.";
        } else if (lower.contains("card") || lower.contains("debit") || lower.contains("credit")) {
            response = "You can request new Debit or Credit cards from the Card Services section.";
        } else if (lower.contains("help") || lower.contains("support")) {
            response = "You can raise a support ticket from the Help & Support section.";
        } else {
            response = "I'm your banking assistant. I can help with balance inquiries, loans, transfers, FD, cards, and more!";
        }

        return ApiResponse.ok(Map.of(
                "query", query,
                "response", response
        ));
    }
}