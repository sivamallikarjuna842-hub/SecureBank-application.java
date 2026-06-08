package com.example.banking.card;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final UserUtil userUtil;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<Card>> requestCard(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam Long accountId,
            @RequestParam String cardType) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(cardService.requestCard(userId, accountId, cardType));
    }

    @PutMapping("/{cardNumber}/activate")
    public ResponseEntity<ApiResponse<Card>> activateCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.activateCard(cardNumber));
    }

    @PutMapping("/{cardNumber}/block")
    public ResponseEntity<ApiResponse<Card>> blockCard(@PathVariable String cardNumber) {
        return ResponseEntity.ok(cardService.blockCard(cardNumber));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Card>>> getMyCards(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(cardService.getUserCards(userId));
    }
}