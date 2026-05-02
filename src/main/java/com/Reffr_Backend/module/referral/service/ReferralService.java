package com.Reffr_Backend.module.referral.service;

import com.Reffr_Backend.module.referral.dto.ReferralRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReferralService {

    // ── REQUEST post — volunteer offers to refer ──────────────────────────
    /** Volunteer on a REQUEST post. Sets PENDING. No chat opened. */
    void createReferral(UUID postId, UUID userId, String volunteerNote);

    // ── OFFER post — applicant submits application ────────────────────────
    /** Apply to an OFFER post with resume + social links. Sets PENDING. No chat. */
    void applyToOffer(UUID postId, UUID userId, ReferralRequestDto.ApplyRequest request);

    // ── Decision (post-type-aware authorization) ──────────────────────────
    /** Accept a PENDING referral. Opens chat. Others become ON_HOLD. */
    void acceptReferral(UUID referralId, UUID userId);

    /** Reject a PENDING referral. */
    void rejectReferral(UUID referralId, UUID userId);

    /** Requester withdraws their own PENDING or ON_HOLD referral. */
    void withdrawReferral(UUID referralId, UUID userId);

    // ── Read ──────────────────────────────────────────────────────────────
    /** My outgoing referral requests (as requester). */
    Page<ReferralRequestDto.Response> getMyRequests(UUID userId, Pageable pageable);

    /** Incoming referral requests (as referrer). */
    Page<ReferralRequestDto.Response> getIncomingRequests(UUID userId, Pageable pageable);

    /** Volunteers on a REQUEST post — seeker view (PENDING + ON_HOLD). */
    Page<ReferralRequestDto.Response> getVolunteers(UUID postId, UUID seekerId, Pageable pageable);

    /** Applicants on an OFFER post — referrer view FIFO (PENDING + ON_HOLD). */
    Page<ReferralRequestDto.Response> getApplicants(UUID postId, UUID referrerId, Pageable pageable);
}
