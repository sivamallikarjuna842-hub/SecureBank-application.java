package com.example.banking.beneficiary;

import com.example.banking.common.ApiResponse;
import com.example.banking.common.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserUtil userUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<Beneficiary>> addBeneficiary(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam String name,
            @RequestParam String accountNumber,
            @RequestParam(required = false) String ifscCode,
            @RequestParam String bankName) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(beneficiaryService.addBeneficiary(userId, name, accountNumber, ifscCode, bankName));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<Beneficiary>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.verifyBeneficiary(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.deleteBeneficiary(id));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Beneficiary>>> getMyBeneficiaries(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        Long userId = userUtil.getCurrentUserId(principal.getUsername());
        return ResponseEntity.ok(beneficiaryService.getUserBeneficiaries(userId));
    }
}