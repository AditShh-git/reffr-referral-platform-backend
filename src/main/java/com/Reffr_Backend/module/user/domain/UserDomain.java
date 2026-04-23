package com.Reffr_Backend.module.user.domain;

import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.entity.UserCompanyVerification;
import com.Reffr_Backend.module.user.entity.VerificationStatus;
import com.Reffr_Backend.module.user.entity.UserSkill;

import java.time.Instant;
import java.util.List;

/**
 * Fix 3: Domain logic lives here, not scattered across services.
 *
 * All user state mutations go through these methods.
 * Services call these — they never touch entity internals directly.
 * This makes the domain model self-describing and testable in isolation.
 */
public final class UserDomain {

    private UserDomain() {}

    public static boolean isInvalidPrimaryEmail(String email) {
        return email != null && email.contains("noreply.github.com");
    }

    public static void replaceSkills(User user, List<String> newSkills) {
        user.getSkills().clear();
        if (newSkills != null) {
            newSkills.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(String::toLowerCase)
                    .distinct()
                    .map(skill -> UserSkill.builder().user(user).skill(skill).build())
                    .forEach(user.getSkills()::add);
        }
    }

    public static void replaceExperiences(User user,
                                          List<ExperienceCommand> commands) {
        user.getCompanyVerifications().clear();
        commands.stream()
                .map(cmd -> UserCompanyVerification.builder()
                        .user(user)
                        .company(cmd.company())
                        .role(cmd.role())
                        .startYear(cmd.startYear())
                        .endYear(cmd.endYear())
                        .current(cmd.current())
                        .verificationStatus(cmd.current() ? VerificationStatus.CURRENT : VerificationStatus.PAST)
                        .verified(false) // Manual entry is unverified by default
                        .build())
                .forEach(user.getCompanyVerifications()::add);
    }

    public static void attachResume(User user, String s3Key,
                                    String originalName) {
        user.setResumeS3Key(s3Key);
        user.setResumeOriginalName(originalName);
        user.setResumeUploadedAt(Instant.now());
    }

    public static void detachResume(User user) {
        user.setResumeS3Key(null);
        user.setResumeOriginalName(null);
        user.setResumeUploadedAt(null);
    }

    public static void recordReferralGiven(User referrer) {
        referrer.incrementReferralsGiven();
    }

    public static void recordSuccessfulReferral(User referrer) {
        referrer.incrementSuccessfulReferrals();
    }

    public static void updateLastSeen(User user) {
        user.setLastSeenAt(Instant.now());
    }

    // ── Command record (replaces DTO inside domain layer) ─────────────

    public record ExperienceCommand(
            String company,
            String role,
            Short  startYear,
            Short  endYear,
            boolean current
    ) {}
}
