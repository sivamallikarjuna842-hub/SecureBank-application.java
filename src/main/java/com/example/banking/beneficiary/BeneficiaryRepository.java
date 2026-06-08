package com.example.banking.beneficiary;

import com.example.banking.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByUser(User user);
    List<Beneficiary> findByUserAndVerified(User user, boolean verified);
    boolean existsByUserAndAccountNumber(User user, String accountNumber);
}