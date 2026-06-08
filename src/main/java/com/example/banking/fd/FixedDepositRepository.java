package com.example.banking.fd;

import com.example.banking.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    List<FixedDeposit> findByUserOrderByCreatedAtDesc(User user);
    List<FixedDeposit> findByMaturedFalseAndClosedFalse();
}