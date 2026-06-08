package com.example.banking.common;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.beneficiary.Beneficiary;
import com.example.banking.beneficiary.BeneficiaryRepository;
import com.example.banking.loan.Loan;
import com.example.banking.loan.LoanRepository;
import com.example.banking.transaction.Transaction;
import com.example.banking.transaction.TransactionRepository;
import com.example.banking.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final LoanRepository loanRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceNumberGenerator refGen;

    @Override
    @Transactional
    public void run(String... args) {
        // Create roles
        if (roleRepository.count() > 0) return;

        Role adminRole = roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN).build());
        Role customerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_CUSTOMER).build());
        roleRepository.save(Role.builder().name(RoleType.ROLE_EMPLOYEE).build());

        // Create admin user
        User admin = User.builder()
                .fullName("Admin User")
                .email("admin@bank.com")
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .enabled(true)
                .phone("9999999999")
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);

        // Create customer user
        User customer = User.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .username("john")
                .password(passwordEncoder.encode("john123"))
                .enabled(true)
                .phone("8888888888")
                .address("123 Main St, City")
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(customer);

        // Create second customer
        User customer2 = User.builder()
                .fullName("Jane Smith")
                .email("jane@example.com")
                .username("jane")
                .password(passwordEncoder.encode("jane123"))
                .enabled(true)
                .phone("7777777777")
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(customer2);

        // Create accounts for customer 1
        Account savingsAcc = Account.builder()
                .accountNumber(refGen.generateAccountNumber())
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("50000.00"))
                .minBalance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer)
                .build();
        accountRepository.save(savingsAcc);

        Account currentAcc = Account.builder()
                .accountNumber(refGen.generateAccountNumber())
                .accountType(AccountType.CURRENT)
                .balance(new BigDecimal("100000.00"))
                .minBalance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer)
                .build();
        accountRepository.save(currentAcc);

        // Create accounts for customer 2
        Account savingsAcc2 = Account.builder()
                .accountNumber(refGen.generateAccountNumber())
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("25000.00"))
                .minBalance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .user(customer2)
                .build();
        accountRepository.save(savingsAcc2);

        // Create sample transactions
        Transaction txn1 = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50000.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("50000.00"))
                .description("Initial deposit")
                .account(savingsAcc)
                .build();
        transactionRepository.save(txn1);

        Transaction txn2 = Transaction.builder()
                .transactionReference(refGen.generateTransactionReference())
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("100000.00"))
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(new BigDecimal("100000.00"))
                .description("Initial deposit")
                .account(currentAcc)
                .build();
        transactionRepository.save(txn2);

        // Create beneficiary
        Beneficiary beneficiary = Beneficiary.builder()
                .name("Jane Smith")
                .accountNumber(savingsAcc2.getAccountNumber())
                .bankName("Test Bank")
                .ifscCode("TEST0001234")
                .verified(true)
                .user(customer)
                .build();
        beneficiaryRepository.save(beneficiary);

        // Create pending loan
        Loan loan = Loan.builder()
                .loanApplicationNumber(refGen.generateLoanApplicationNumber())
                .amount(new BigDecimal("200000.00"))
                .interestRate(new BigDecimal("10.5"))
                .tenureMonths(24)
                .emiAmount(calculateSampleEMI(new BigDecimal("200000"), new BigDecimal("10.5"), 24))
                .purpose("Home renovation")
                .status(LoanStatus.PENDING)
                .user(customer)
                .build();
        loanRepository.save(loan);

        System.out.println("===== BANKING APPLICATION SEED DATA =====");
        System.out.println("Admin: username=admin, password=admin123");
        System.out.println("Customer 1: username=john, password=john123");
        System.out.println("Customer 2: username=jane, password=jane123");
        System.out.println("Savings Acc (John): " + savingsAcc.getAccountNumber());
        System.out.println("Current Acc (John): " + currentAcc.getAccountNumber());
        System.out.println("Savings Acc (Jane): " + savingsAcc2.getAccountNumber());
        System.out.println("=========================================");
    }

    private BigDecimal calculateSampleEMI(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12 * 100), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(months);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP);
    }
}