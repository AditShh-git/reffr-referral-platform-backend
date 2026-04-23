package com.Reffr_Backend.module.referral.repository;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, UUID> {

    // ── Duplicate check ──────────────────────────────────────────────
    boolean existsByPostIdAndRequesterIdAndReferrerId(
            UUID postId, UUID requesterId, UUID referrerId
    );

    boolean existsByPostIdAndRequesterIdAndStatus(
            UUID postId, UUID requesterId, com.Reffr_Backend.module.referral.entity.ReferralStatus status
    );

    // ── My requests (job seeker) ─────────────────────────────────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Page<ReferralRequest> findByRequesterId(UUID requesterId, Pageable pageable);

    // ── Incoming requests (referrer) ─────────────────────────────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Page<ReferralRequest> findByReferrerId(UUID referrerId, Pageable pageable);

    // ── Single fetch (for accept/reject/withdraw) ────────────────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Optional<ReferralRequest> findById(UUID id);

    @Query("""
    SELECT r FROM ReferralRequest r
    JOIN FETCH r.post
    JOIN FETCH r.requester
    JOIN FETCH r.referrer
    WHERE r.id = :id
""")
    Optional<ReferralRequest> findByIdWithDetails(@Param("id") UUID id);
}
