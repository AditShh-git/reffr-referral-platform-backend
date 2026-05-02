package com.Reffr_Backend.module.referral.entity;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "referral_requests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_referral_unique",
                        columnNames = {"post_id", "requester_id", "referrer_id"})
        },
        indexes = {
                @Index(name = "idx_ref_post",       columnList = "post_id"),
                @Index(name = "idx_ref_requester",  columnList = "requester_id"),
                @Index(name = "idx_ref_referrer",   columnList = "referrer_id"),
                @Index(name = "idx_ref_status",     columnList = "status"),
                @Index(name = "idx_ref_expires",    columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id")
    private User referrer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReferralStatus status;

    // ── Messaging ─────────────────────────────────────────────────────────

    /** Short note added by the volunteer when offering to refer (REQUEST posts). */
    @Column(name = "volunteer_note", columnDefinition = "TEXT")
    private String volunteerNote;

    /**
     * Generic message field (kept for backward compatibility).
     * Prefer {@code volunteerNote} for REQUEST posts.
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String content;

    // ── OFFER apply data ──────────────────────────────────────────────────

    /**
     * S3 key of the applicant's resume, snapshotted at apply-time for OFFER posts.
     * Stored on the referral record — not on the post — because only the accepted
     * applicant's resume should be visible to the referrer.
     */
    @Column(name = "resume_snapshot_key", length = 500)
    private String resumeSnapshotKey;

    @Column(name = "applicant_github_link", length = 500)
    private String applicantGithubLink;

    @Column(name = "applicant_linkedin_link", length = 500)
    private String applicantLinkedinLink;

    // ── Expiry ────────────────────────────────────────────────────────────

    /**
     * When this PENDING referral auto-expires if no action is taken.
     * Set by the service layer based on {@code app.referral.expiry-days}.
     * The scheduler transitions PENDING → EXPIRED when this passes.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Timestamps ────────────────────────────────────────────────────────

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}