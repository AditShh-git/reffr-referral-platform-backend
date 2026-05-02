package com.Reffr_Backend.module.referral.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable append-only audit trail for every referral status transition.
 *
 * <p>Written on: CREATED, ACCEPTED, REJECTED, WITHDRAWN, ON_HOLD, EXPIRED.
 * Actor is null for system-initiated events (scheduler, etc.).
 *
 * <p>Designed for easy migration to a domain-event approach later —
 * the shape (referralId, actor, action, old/new status) maps cleanly
 * to an event record.
 */
@Entity
@Table(
        name = "referral_audit_log",
        indexes = {
                @Index(name = "idx_audit_referral", columnList = "referral_id, created_at"),
                @Index(name = "idx_audit_actor",    columnList = "actor_id")
        }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_id", nullable = false)
    private ReferralRequest referral;

    /** The user who triggered the transition. NULL for system-initiated events. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Human-readable action label.
     * Examples: CREATED, ACCEPTED, REJECTED, WITHDRAWN, ON_HOLD, EXPIRED.
     */
    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    /** Optional context for why this transition happened. */
    @Column(columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Factory helpers ───────────────────────────────────────────────────

    public static ReferralAuditLog of(ReferralRequest referral,
                                      User actor,
                                      String action,
                                      ReferralStatus oldStatus,
                                      ReferralStatus newStatus,
                                      String note) {
        return ReferralAuditLog.builder()
                .referral(referral)
                .actor(actor)
                .action(action)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus != null ? newStatus.name() : null)
                .note(note)
                .build();
    }
}
