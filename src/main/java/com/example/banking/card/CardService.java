package com.example.banking.card;

import com.example.banking.account.Account;
import com.example.banking.account.AccountRepository;
import com.example.banking.common.ApiResponse;
import com.example.banking.common.ReferenceNumberGenerator;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ReferenceNumberGenerator refGen;

    public ApiResponse<Card> requestCard(Long userId, Long accountId, String cardType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            return ApiResponse.error("Account does not belong to this user");
        }

        Card card = Card.builder()
                .cardNumber(refGen.generateCardNumber())
                .cardHolderName(user.getFullName())
                .expiryDate(LocalDate.now().plusYears(5))
                .cvv(refGen.generateCvv())
                .active(false)
                .blocked(false)
                .cardType(cardType.toUpperCase())
                .creditLimit(cardType.equalsIgnoreCase("CREDIT") ? BigDecimal.valueOf(50000) : null)
                .user(user)
                .account(account)
                .build();

        cardRepository.save(card);
        return ApiResponse.ok(cardType + " card requested successfully. Activate once received.", card);
    }

    public ApiResponse<Card> activateCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (card.isBlocked()) {
            return ApiResponse.error("Card is blocked. Cannot activate.");
        }

        card.setActive(true);
        cardRepository.save(card);
        return ApiResponse.ok("Card activated successfully", card);
    }

    public ApiResponse<Card> blockCard(String cardNumber) {
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setBlocked(true);
        card.setActive(false);
        cardRepository.save(card);
        return ApiResponse.ok("Card blocked successfully", card);
    }

    public ApiResponse<List<Card>> getUserCards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(cardRepository.findByUser(user));
    }
}