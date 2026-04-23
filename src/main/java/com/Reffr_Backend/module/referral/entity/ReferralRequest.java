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
                @Index(name = "idx_ref_post", columnList = "post_id"),
                @Index(name = "idx_ref_requester", columnList = "requester_id"),
                @Index(name = "idx_ref_referrer", columnList = "referrer_id"),
                @Index(name = "idx_ref_status", columnList = "status")
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

    @Column(name = "message", columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}