package com.Reffr_Backend.module.referral.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import com.Reffr_Backend.module.referral.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.Reffr_Backend.common.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Apply / Refer / Manage referrals")
@RequiresOnboarding
public class ReferralController {

    private final ReferralService referralService;

    // ── Create (Apply / Refer) ────────────────────────────────────────
    @PostMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Apply or Refer based on post type")
    public ResponseEntity<ApiResponse<Void>> createReferral(
            @PathVariable UUID postId,
            @Valid @RequestBody(required = false) ReferralRequestDto.CreateRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        String message = request != null ? request.getContent() : null;

        referralService.createReferral(postId, userId, message);

        return ResponseEntity.ok(ApiResponse.success("Request sent", null));
    }

    // ── Accept ────────────────────────────────────────────────────────
    @PatchMapping("/{referralId}/accept")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Accept referral request (referrer only)")
    public ResponseEntity<ApiResponse<Void>> acceptReferral(
            @PathVariable UUID referralId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        referralService.acceptReferral(referralId, userId);

        return ResponseEntity.ok(ApiResponse.success("Referral accepted", null));
    }

    // ── Reject ────────────────────────────────────────────────────────
    @PatchMapping("/{referralId}/reject")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reject referral request (referrer only)")
    public ResponseEntity<ApiResponse<Void>> rejectReferral(
            @PathVariable UUID referralId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        referralService.rejectReferral(referralId, userId);

        return ResponseEntity.ok(ApiResponse.success("Referral rejected", null));
    }

    // ── My Requests (Job Seeker) ──────────────────────────────────────
    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My referral requests (job seeker view)")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Pageable pageable = PaginationUtils.of(page, size);

        Page<ReferralRequestDto.Response> result =
                referralService.getMyRequests(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Incoming Requests (Referrer Dashboard) ────────────────────────
    @GetMapping("/incoming")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Incoming referral requests (referrer view)")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getIncomingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Pageable pageable = PaginationUtils.of(page, size);

        Page<ReferralRequestDto.Response> result =
                referralService.getIncomingRequests(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{referralId}/withdraw")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Withdraw referral request (requester only)")
    public ResponseEntity<ApiResponse<Void>> withdrawReferral(
            @PathVariable UUID referralId
    ) {
        referralService.withdrawReferral(referralId, SecurityUtils.getCurrentUserId());

        return ResponseEntity.ok(ApiResponse.success("Referral withdrawn", null));
    }
}
