package com.example.banking.transaction;

import com.example.banking.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountOrderByTransactionDateDesc(Account account);

    List<Transaction> findByAccountAndTransactionDateBetweenOrderByTransactionDateDesc(
            Account account, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account = :account " +
           "AND t.transactionDate >= :since AND t.transactionType IN ('WITHDRAWAL', 'TRANSFER_OUT')")
    Double sumOutgoingSince(@Param("account") Account account, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account = :account " +
           "AND t.transactionDate >= :since AND t.transactionType IN ('WITHDRAWAL', 'TRANSFER_OUT')")
    Long countOutgoingSince(@Param("account") Account account, @Param("since") LocalDateTime since);

    List<Transaction> findByFlaggedTrue();
}