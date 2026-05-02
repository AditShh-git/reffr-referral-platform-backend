package com.Reffr_Backend.module.referral.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import com.Reffr_Backend.common.util.PaginationUtils;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import com.Reffr_Backend.module.referral.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Volunteer / Apply / Accept / Manage referrals")
@RequiresOnboarding
public class ReferralController {

    private final ReferralService referralService;

    // ── Volunteer on REQUEST post ────────────────────────────────────────
    @PostMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Volunteer to refer (REQUEST posts only)",
            description = "Sets status=PENDING. Does NOT open chat. Seeker reviews volunteers and accepts one.")
    public ResponseEntity<ApiResponse<Void>> createReferral(
            @PathVariable UUID postId,
            @Valid @RequestBody(required = false) ReferralRequestDto.CreateRequest request
    ) {
        String note = request != null ? request.getContent() : null;
        referralService.createReferral(postId, SecurityUtils.getCurrentUserId(), note);
        return ResponseEntity.ok(ApiResponse.success("Volunteer request sent", null));
    }

    // ── Apply to OFFER post ─────────────────────────────────────────────
    @PostMapping("/{postId}/apply")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Apply to an OFFER post",
            description = "Submits resume snapshot + social links. Sets status=PENDING. No chat until referrer accepts.")
    public ResponseEntity<ApiResponse<Void>> applyToOffer(
            @PathVariable UUID postId,
            @Valid @RequestBody(required = false) ReferralRequestDto.ApplyRequest request
    ) {
        ReferralRequestDto.ApplyRequest req = request != null ? request : new ReferralRequestDto.ApplyRequest();
        referralService.applyToOffer(postId, SecurityUtils.getCurrentUserId(), req);
        return ResponseEntity.ok(ApiResponse.success("Application submitted", null));
    }

    // ── Accept ──────────────────────────────────────────────────────────
    @PatchMapping("/{referralId}/accept")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Accept a referral",
            description = "REQUEST: seeker accepts a volunteer. OFFER: referrer accepts an applicant. Opens chat.")
    public ResponseEntity<ApiResponse<Void>> acceptReferral(@PathVariable UUID referralId) {
        referralService.acceptReferral(referralId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Referral accepted — chat is now open", null));
    }

    // ── Reject ──────────────────────────────────────────────────────────
    @PatchMapping("/{referralId}/reject")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reject a referral (decision-maker only)")
    public ResponseEntity<ApiResponse<Void>> rejectReferral(@PathVariable UUID referralId) {
        referralService.rejectReferral(referralId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Referral rejected", null));
    }

    // ── Withdraw ────────────────────────────────────────────────────────
    @PatchMapping("/{referralId}/withdraw")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Withdraw your own referral request / volunteer offer")
    public ResponseEntity<ApiResponse<Void>> withdrawReferral(@PathVariable UUID referralId) {
        referralService.withdrawReferral(referralId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Referral withdrawn", null));
    }

    // ── My outgoing ─────────────────────────────────────────────────────
    @GetMapping("/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My referral requests / applications (as requester)")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                referralService.getMyRequests(SecurityUtils.getCurrentUserId(), pageable)));
    }

    // ── Incoming ────────────────────────────────────────────────────────
    @GetMapping("/incoming")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Incoming referral requests / applications (as referrer)")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getIncomingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                referralService.getIncomingRequests(SecurityUtils.getCurrentUserId(), pageable)));
    }

    // ── Volunteers on REQUEST post (seeker sees who volunteered) ─────────
    @GetMapping("/post/{postId}/volunteers")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "View volunteers on a REQUEST post (seeker/owner only)",
            description = "Returns PENDING and ON_HOLD volunteers so seeker can review and accept one.")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getVolunteers(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                referralService.getVolunteers(postId, SecurityUtils.getCurrentUserId(), pageable)));
    }

    // ── Applicants on OFFER post (referrer sees who applied — FIFO) ──────
    @GetMapping("/post/{postId}/applicants")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "View applicants on an OFFER post (referrer/owner only)",
            description = "Returns PENDING and ON_HOLD applicants in FIFO order. Future versions will support reputation-based sorting.")
    public ResponseEntity<ApiResponse<Page<ReferralRequestDto.Response>>> getApplicants(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                referralService.getApplicants(postId, SecurityUtils.getCurrentUserId(), pageable)));
    }
}
