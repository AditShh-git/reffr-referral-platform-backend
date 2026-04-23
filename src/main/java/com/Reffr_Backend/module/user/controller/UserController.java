package com.Reffr_Backend.module.user.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.service.UserProfileService;
import com.Reffr_Backend.module.user.service.UserResumeService;
import com.Reffr_Backend.module.user.service.UserSearchService;
import com.Reffr_Backend.module.user.service.CompanyVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.Reffr_Backend.common.util.PaginationUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserProfileService userProfileService;
    private final UserResumeService userResumeService;
    private final UserSearchService userSearchService;
    private final CompanyVerificationService companyVerificationService;

    // ── Own profile ────────────────────────────────────────────────

    @Operation(summary = "Get my profile", description = "Fetch authenticated user's profile details")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.getMyProfile()
        ));
    }

    // ── Onboarding ─────────────────────────────────────────────────

    @Operation(summary = "Complete onboarding", description = "Submit initial profile details after sign up")
    @PostMapping("/me/onboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> onboardUser(
            @Valid @RequestBody UserDto.OnboardingRequest request) {
        
        userProfileService.onboardUser(request);
        return ResponseEntity.ok(ApiResponse.success("Onboarding completed successfully", null));
    }

    @Operation(summary = "Get onboarding status", description = "Check if the authenticated user has completed onboarding")
    @GetMapping("/me/onboarding-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> getOnboardingStatus() {
        boolean status = userProfileService.getOnboardingStatus();
        return ResponseEntity.ok(ApiResponse.success(java.util.Map.of("completed", status)));
    }

    @Operation(summary = "Update profile", description = "Update authenticated user's profile (partial update supported)")
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> updateProfile(
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                userProfileService.updateProfile(request)
        ));
    }

    @Operation(summary = "Delete account", description = "Deactivate or delete authenticated user's account")
    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        userProfileService.deleteMyAccount();
        return ResponseEntity.ok(ApiResponse.success("Account deleted", null));
    }

    // ── Resume ─────────────────────────────────────────────────────

    @Operation(summary = "Upload resume", description = "Upload resume file (PDF/DOC/DOCX, max 5MB)")
    @PostMapping(value = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> uploadResume(
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(ApiResponse.success(
                "Resume uploaded successfully",
                userResumeService.upload(file)
        ));
    }

    @Operation(summary = "Delete resume", description = "Delete uploaded resume of authenticated user")
    @DeleteMapping("/me/resume")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteResume() {
        userResumeService.delete();
        return ResponseEntity.ok(ApiResponse.success("Resume deleted", null));
    }

    @Operation(summary = "Get my resume URL", description = "Get secure access URL for authenticated user's resume")
    @GetMapping("/me/resume/url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getMyResumeUrl() {
        return ResponseEntity.ok(ApiResponse.success(
                userResumeService.generateAccessUrl(
                        SecurityUtils.getCurrentUserGithubUsername()
                )
        ));
    }

    // ── Company Verification & Experience ──

    @Operation(summary = "Add work experience", description = "Add a work experience entry (unverified by default)")
    @PostMapping("/me/experience")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addExperience(
            @Valid @RequestBody UserDto.ExperienceRequest request) {
        
        companyVerificationService.addExperience(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Experience added successfully", null));
    }

    @Operation(summary = "Send company verification OTP", description = "Sends an OTP to the user's corporate email")
    @PostMapping("/me/company/send-otp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @RequestParam String email) {
        
        companyVerificationService.sendVerificationEmail(SecurityUtils.getCurrentUserId(), email);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP sent", null));
    }

    @Operation(summary = "Verify company OTP", description = "Verifies the corporate email OTP and marks company as verified")
    @PostMapping("/me/company/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> verifyCompany(
            @RequestParam String otp) {
        
        companyVerificationService.verifyOtp(SecurityUtils.getCurrentUserId(), otp);
        return ResponseEntity.ok(ApiResponse.success("Company verified successfully", null));
    }

    @Operation(summary = "Add public profile proof", description = "Add a link to a public profile (LinkedIn/etc) for verification")
    @PostMapping("/me/company/public")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addPublicProof(
            @Valid @RequestBody UserDto.PublicProofRequest request) {
        
        companyVerificationService.addPublicProof(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Public proof added", null));
    }

    @Operation(summary = "Upload verification document", description = "Submit a document key for verification")
    @PostMapping("/me/company/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> uploadDocument(
            @Valid @RequestBody UserDto.DocumentProofRequest request) {
        
        companyVerificationService.uploadDocument(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Document submitted for verification", null));
    }

    @Operation(summary = "Get verification history", description = "Fetch all verification and experience records for the user")
    @GetMapping("/me/verifications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.List<UserDto.VerificationResponse>>> getVerifications() {
        return ResponseEntity.ok(ApiResponse.success(
                companyVerificationService.getUserVerifications(SecurityUtils.getCurrentUserId())
        ));
    }

    // ── Public profile ─────────────────────────────────────────────

    @Operation(summary = "Get public profile", description = "Fetch public profile of a user by username")
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserDto.PublicProfileResponse>> getPublicProfile(
            @PathVariable String username) {

        return ResponseEntity.ok(ApiResponse.success(
                userProfileService.getPublicProfile(username)
        ));
    }

    @Operation(summary = "Get resume URL", description = "Get secure resume URL of a user (access controlled)")
    @GetMapping("/{username}/resume/url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> getResumeUrl(
            @PathVariable String username) {

        return ResponseEntity.ok(ApiResponse.success(
                userResumeService.generateAccessUrl(username)
        ));
    }

    // ── Search ─────────────────────────────────────────────────────

    @Operation(summary = "Search users", description = "Search users by query with pagination")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserDto.UserSummary>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PaginationUtils.of(
                page, size, Sort.by("totalReferralsGiven").descending()
        );

        return ResponseEntity.ok(ApiResponse.success(
                userSearchService.searchUsers(q, pageable)
        ));
    }

    @Operation(summary = "Find referrers", description = "Find verified referrers by company with pagination")
    @GetMapping("/referrers")
    public ResponseEntity<ApiResponse<Page<UserDto.PublicProfileResponse>>> findReferrers(
            @RequestParam String company,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PaginationUtils.of(
                page, size, Sort.by("successfulReferrals").descending()
        );

        return ResponseEntity.ok(ApiResponse.success(
                userSearchService.findVerifiedReferrers(company, pageable)
        ));
    }
}