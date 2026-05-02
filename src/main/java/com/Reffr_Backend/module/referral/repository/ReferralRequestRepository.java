package com.Reffr_Backend.module.referral.repository;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRequestRepository extends JpaRepository<ReferralRequest, UUID> {

    // ── Duplicate check ───────────────────────────────────────────────────
    boolean existsByPostIdAndRequesterIdAndReferrerId(UUID postId, UUID requesterId, UUID referrerId);

    boolean existsByPostIdAndRequesterIdAndStatus(UUID postId, UUID requesterId, ReferralStatus status);

    // ── Limit checks ─────────────────────────────────────────────────────
    /** Count volunteers/applicants currently in a countable state for a post. */
    long countByPostIdAndStatusIn(UUID postId, List<ReferralStatus> statuses);

    long countByPostIdAndStatus(UUID postId, ReferralStatus status);

    // ── My requests (job seeker) ─────────────────────────────────────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Page<ReferralRequest> findByRequesterId(UUID requesterId, Pageable pageable);

    // ── Incoming requests (referrer) ─────────────────────────────────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Page<ReferralRequest> findByReferrerId(UUID referrerId, Pageable pageable);

    // ── Volunteers on a REQUEST post (seeker sees who volunteered) ────────
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    Page<ReferralRequest> findByPostIdAndStatusOrderByCreatedAtAsc(
            UUID postId, ReferralStatus status, Pageable pageable);

    /**
     * All non-terminal entries for a post — used when seeker wants to see
     * PENDING + ON_HOLD volunteers together.
     */
    @EntityGraph(attributePaths = {"post", "requester", "referrer"})
    @Query("""
        SELECT r FROM ReferralRequest r
        WHERE r.post.id = :postId
          AND r.status IN :statuses
        ORDER BY r.createdAt ASC
    """)
    Page<ReferralRequest> findByPostIdAndStatusIn(
            @Param("postId")   UUID postId,
            @Param("statuses") List<ReferralStatus> statuses,
            Pageable pageable);

    // ── Single fetch (for accept/reject/withdraw) ─────────────────────────
    @Query("""
        SELECT r FROM ReferralRequest r
        JOIN FETCH r.post
        JOIN FETCH r.requester
        JOIN FETCH r.referrer
        WHERE r.id = :id
    """)
    Optional<ReferralRequest> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM ReferralRequest r
        WHERE r.post.id = :postId
          AND r.status = com.Reffr_Backend.module.referral.entity.ReferralStatus.ACCEPTED
          AND (r.requester.id = :userId OR r.referrer.id = :userId)
    """)
    boolean existsAcceptedParticipant(@Param("postId") UUID postId, @Param("userId") UUID userId);

    // ── Reputation / metrics queries ──────────────────────────────────────
    long countByReferrerIdAndStatus(UUID referrerId, ReferralStatus status);

    long countByReferrerId(UUID referrerId);

    // ── ON_HOLD bulk update ────────────────────────────────────────────────
    /**
     * When one volunteer is accepted, all other PENDING volunteers on the same
     * post become ON_HOLD so the seeker retains backup options.
     */
    @Modifying
    @Query("""
        UPDATE ReferralRequest r
           SET r.status = com.Reffr_Backend.module.referral.entity.ReferralStatus.ON_HOLD
         WHERE r.post.id     = :postId
           AND r.requester.id = :requesterId
           AND r.status      = com.Reffr_Backend.module.referral.entity.ReferralStatus.PENDING
           AND r.id          != :acceptedId
    """)
    int putOthersOnHold(@Param("postId")     UUID postId,
                        @Param("requesterId") UUID requesterId,
                        @Param("acceptedId")  UUID acceptedId);

    // ── Expiry scheduler ──────────────────────────────────────────────────
    @Modifying
    @Query("""
        UPDATE ReferralRequest r
           SET r.status = com.Reffr_Backend.module.referral.entity.ReferralStatus.EXPIRED
         WHERE r.status    = com.Reffr_Backend.module.referral.entity.ReferralStatus.PENDING
           AND r.expiresAt < :cutoff
    """)
    int expirePendingOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Returns expired referral IDs so the audit service can log each one.
     * Called before the bulk-update, within the same transaction.
     */
    @Query("""
        SELECT r FROM ReferralRequest r
        JOIN FETCH r.post
        JOIN FETCH r.requester
        JOIN FETCH r.referrer
        WHERE r.status    = com.Reffr_Backend.module.referral.entity.ReferralStatus.PENDING
          AND r.expiresAt < :cutoff
    """)
    List<ReferralRequest> findExpiredPending(@Param("cutoff") LocalDateTime cutoff);
}
