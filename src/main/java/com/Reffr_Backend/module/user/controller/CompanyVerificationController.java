package com.Reffr_Backend.module.user.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.service.CompanyVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/company")
@RequiredArgsConstructor
public class CompanyVerificationController {

    private final CompanyVerificationService companyVerificationService;

    @Operation(summary = "Send company verification OTP")
    @PostMapping("/send-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@RequestParam String email) {
        companyVerificationService.sendVerificationEmail(SecurityUtils.getCurrentUserId(), email);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent", null));
    }

    @Operation(summary = "Verify company OTP")
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyCompany(@RequestParam String otp) {
        companyVerificationService.verifyOtp(SecurityUtils.getCurrentUserId(), otp);
        return ResponseEntity.ok(ApiResponse.success("Company verified successfully", null));
    }

    @Operation(summary = "Upload company verification document")
    @PostMapping("/document-upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @Valid @RequestBody UserDto.DocumentProofRequest request) {
        companyVerificationService.uploadDocument(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Document submitted for verification", null));
    }
}
