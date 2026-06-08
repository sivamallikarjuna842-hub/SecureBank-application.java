package com.example.banking.beneficiary;

import com.example.banking.common.ApiResponse;
import com.example.banking.user.User;
import com.example.banking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    public ApiResponse<Beneficiary> addBeneficiary(Long userId, String name, String accountNumber,
                                                    String ifscCode, String bankName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (beneficiaryRepository.existsByUserAndAccountNumber(user, accountNumber)) {
            return ApiResponse.error("Beneficiary with this account number already exists");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .name(name)
                .accountNumber(accountNumber)
                .ifscCode(ifscCode)
                .bankName(bankName)
                .verified(false)
                .user(user)
                .build();

        beneficiaryRepository.save(beneficiary);
        return ApiResponse.ok("Beneficiary added. Verification required.", beneficiary);
    }

    public ApiResponse<Beneficiary> verifyBeneficiary(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
        beneficiary.setVerified(true);
        beneficiaryRepository.save(beneficiary);
        return ApiResponse.ok("Beneficiary verified successfully", beneficiary);
    }

    public ApiResponse<Void> deleteBeneficiary(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));
        beneficiaryRepository.delete(beneficiary);
        return ApiResponse.ok("Beneficiary deleted successfully", null);
    }

    public ApiResponse<List<Beneficiary>> getUserBeneficiaries(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ApiResponse.ok(beneficiaryRepository.findByUser(user));
    }
}