package com.Reffr_Backend.module.feed.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_status",     columnList = "post_status"),
                @Index(name = "idx_posts_type",       columnList = "type"),
                @Index(name = "idx_posts_company",    columnList = "company"),
                @Index(name = "idx_posts_active",     columnList = "is_active, created_at DESC"),
                @Index(name = "idx_posts_created_at", columnList = "created_at DESC")
        }
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Column(length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 150)
    private String company;

    @Column(name = "role", length = 150)
    private String currentRole;

    @Column(name = "min_experience")
    private Integer minExperience;

    @Column(name = "max_experience")
    private Integer maxExperience;

    @Column(length = 150)
    private String location;

    // ── Social / Resume (REQUEST posts) ──────────────────────────────────

    /**
     * S3 key snapshotted from the author's current resume at post-creation time.
     * Never updated after creation — provides historical consistency even if
     * the user uploads a newer resume later.
     */
    @Column(name = "resume_snapshot_key", length = 500)
    private String resumeSnapshotKey;

    @Column(name = "github_link", length = 500)
    private String githubLink;

    @Column(name = "linkedin_link", length = 500)
    private String linkedinLink;

    /**
     * Controls who can see the resume snapshot URL in the post response.
     * PUBLIC        → all authenticated users see it.
     * VERIFIED_ONLY → field is nulled-out for unverified viewers.
     * PRIVATE       → never exposed; seeker shares directly in chat.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resume_visibility", nullable = false, length = 20)
    @Builder.Default
    private PostVisibility resumeVisibility = PostVisibility.PUBLIC;

    @Column(name = "urgency_deadline")
    private Instant urgencyDeadline;

    // ── Limits ────────────────────────────────────────────────────────────

    /** Max volunteers allowed before post becomes FULL (REQUEST posts). */
    @Column(name = "max_volunteers", nullable = false)
    @Builder.Default
    private int maxVolunteers = 10;

    /** Max applicants allowed before post becomes FULL (OFFER posts). */
    @Column(name = "max_applicants", nullable = false)
    @Builder.Default
    private int maxApplicants = 20;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Fine-grained lifecycle status (replaces old {@code isClosed} boolean).
     * <pre>
     *  OPEN      → accepting new volunteers/applicants
     *  FULL      → limit reached; no new entries allowed
     *  CLOSED    → manually closed by author
     *  EXPIRED   → expiresAt passed with no accepted referral
     *  FULFILLED → at least one accepted referral progressed to REFERRED
     * </pre>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "post_status", nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.OPEN;

    /** Soft-delete flag — separate from lifecycle status. */
    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    // ── Metrics ───────────────────────────────────────────────────────────

    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "referral_count")
    @Builder.Default
    private Integer referralCount = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @org.hibernate.annotations.BatchSize(size = 25)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostTag> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ── Nested enums ──────────────────────────────────────────────────────

    public enum PostType { REQUEST, OFFER }

    // ── Domain helpers ────────────────────────────────────────────────────

    public boolean isOwnedBy(UUID userId) {
        return this.author != null && this.author.getId().equals(userId);
    }

    /** True only when the post is OPEN (not FULL, CLOSED, EXPIRED, or FULFILLED). */
    public boolean isAcceptingEntries() {
        return this.status == PostStatus.OPEN;
    }

    public void markFull()      { this.status = PostStatus.FULL;      }
    public void markFulfilled() { this.status = PostStatus.FULFILLED;  }
    public void close()         { this.status = PostStatus.CLOSED;     }
    public void markExpired()   { this.status = PostStatus.EXPIRED;    }

    public void incrementReferralCount() { this.referralCount++; }
    public void incrementViewsCount()    { this.viewsCount++;    }
}
