package com.Reffr_Backend.module.referral.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_feedback",
        uniqueConstraints = @UniqueConstraint(columnNames = "referral_id")) // one feedback per referral
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // The referral this feedback is about (1-1 enforced by UNIQUE constraint)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_id", nullable = false, unique = true)
    private ReferralRequest referral;

    // The seeker who gives the feedback
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // The referrer whose reputation is affected
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FeedbackRating rating;

    @Column(length = 200)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public enum FeedbackRating { UP, DOWN }
}
