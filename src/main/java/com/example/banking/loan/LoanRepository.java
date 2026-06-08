package com.example.banking.loan;

import com.example.banking.common.LoanStatus;
import com.example.banking.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserOrderByAppliedAtDesc(User user);
    List<Loan> findByStatusOrderByAppliedAtAsc(LoanStatus status);
    List<Loan> findByUserAndStatus(User user, LoanStatus status);
}