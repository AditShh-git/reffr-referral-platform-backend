package com.Reffr_Backend.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "github_id", unique = true, nullable = false, length = 100)
    private String githubId;

    @Column(name = "github_username", unique = true, nullable = false, length = 100)
    private String githubUsername;

    @Column(name = "github_url", length = 255)
    private String githubUrl;

    @Column(name = "email", unique = true, length = 255)
    private String primaryEmail;

    @Column(name = "is_email_verified")
    @Builder.Default
    private boolean isEmailVerified = false;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 150)
    private String location;

    // ── Resume ──
    @Column(name = "resume_s3_key", length = 500)
    private String resumeS3Key;

    @Column(name = "resume_original_name", length = 255)
    private String resumeOriginalName;

    @Column(name = "resume_uploaded_at")
    private Instant resumeUploadedAt;

    // ── Experience ──
    @Column(name = "current_company", length = 150)
    private String currentCompany;

    @Column(name = "current_job_role", length = 150)
    private String currentRole;

    @Column(name = "years_of_experience")
    @Builder.Default
    private Short yearsOfExperience = 0;

    // ── Company verification & History ──
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<UserCompanyVerification> companyVerifications = new java.util.ArrayList<>();

    // ── Referral reputation ──
    @Column(name = "total_referrals_given")
    @Builder.Default
    private Integer totalReferralsGiven = 0;

    @Column(name = "successful_referrals")
    @Builder.Default
    private Integer successfulReferrals = 0;

    @Column(name = "positive_feedback_count")
    @Builder.Default
    private Integer positiveFeedbackCount = 0;

    @Column(name = "negative_feedback_count")
    @Builder.Default
    private Integer negativeFeedbackCount = 0;

    // ── Skills (one-to-many) ──
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserSkill> skills = new ArrayList<>();


    // ── System ──
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", columnDefinition = "user_role")
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "onboarding_completed")
    @Builder.Default
    private boolean onboardingCompleted = false;

    // ── Email preferences ──
    @Column(name = "email_notifications_enabled")
    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    // Per-type throttle anchors — avoids one type blocking another
    @Column(name = "last_chat_email_at")
    private Instant lastChatEmailAt;

    @Column(name = "last_referral_email_at")
    private Instant lastReferralEmailAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ── Enums ──
    public enum UserRole            { USER, ADMIN }

    // ── Domain helpers ──
    public double getReferralSuccessRate() {
        int total = (positiveFeedbackCount != null ? positiveFeedbackCount : 0)
                  + (negativeFeedbackCount != null ? negativeFeedbackCount : 0);
        if (total == 0) return 0.0;
        return (double) positiveFeedbackCount / total * 100;
    }

    public void applyPositiveFeedback() {
        this.positiveFeedbackCount = (this.positiveFeedbackCount != null ? this.positiveFeedbackCount : 0) + 1;
        this.totalReferralsGiven   = (this.totalReferralsGiven   != null ? this.totalReferralsGiven   : 0) + 1;
    }

    public void applyNegativeFeedback() {
        this.negativeFeedbackCount = (this.negativeFeedbackCount != null ? this.negativeFeedbackCount : 0) + 1;
        this.totalReferralsGiven   = (this.totalReferralsGiven   != null ? this.totalReferralsGiven   : 0) + 1;
    }

    public void incrementReferralsGiven() {
        this.totalReferralsGiven++;
    }

    public void incrementSuccessfulReferrals() {
        this.successfulReferrals++;
    }

    public boolean hasResume() {
        return resumeS3Key != null && !resumeS3Key.isBlank();
    }

    public boolean isVerified() {
        return companyVerifications.stream()
                .anyMatch(v -> v.isVerified() && v.getVerificationStatus() == VerificationStatus.CURRENT);
    }
}
