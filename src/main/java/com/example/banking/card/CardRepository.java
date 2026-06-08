package com.example.banking.card;

import com.example.banking.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUser(User user);
    Optional<Card> findByCardNumber(String cardNumber);
}