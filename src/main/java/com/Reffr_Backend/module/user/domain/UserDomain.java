package com.Reffr_Backend.module.user.domain;

import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.entity.UserCompanyVerification;
import com.Reffr_Backend.module.user.entity.VerificationStatus;
import com.Reffr_Backend.module.user.entity.UserSkill;
import com.Reffr_Backend.module.user.dto.UserDto;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
                    .distinct()
                    .map(skill -> UserSkill.builder()
                            .user(user)
                            .skillName(skill.trim())
                            .category(inferCategory(skill).orElse(null))
                            .verified(true)
                            .build())
                    .forEach(user.getSkills()::add);
        }
    }

    public static void replaceParsedSkills(User user, List<UserDto.ParsedResumeResponse.ParsedSkill> parsedSkills, boolean isVerified) {
        user.getSkills().clear();
        if (parsedSkills != null) {
            parsedSkills.stream()
                    .filter(skill -> skill != null && skill.getName() != null && !skill.getName().isBlank())
                    .map(skill -> UserSkill.builder()
                            .user(user)
                            .skillName(skill.getName().trim())
                            .category(skill.getCategory())
                            .verified(isVerified)
                            .build())
                    .forEach(user.getSkills()::add);
        }
    }

    public static void appendExperiences(User user,
                                          List<ExperienceCommand> commands) {
        if (commands == null) return;
        for (ExperienceCommand cmd : commands) {
            boolean isDuplicate = user.getCompanyVerifications().stream().anyMatch(v ->
                    v.getCompany().equalsIgnoreCase(cmd.company()) &&
                    v.getRole().equalsIgnoreCase(cmd.role()) &&
                    java.util.Objects.equals(v.getStartYear(), cmd.startYear()));
            if (isDuplicate) continue;

            if (cmd.current()) {
                user.getCompanyVerifications().stream()
                        .filter(UserCompanyVerification::isCurrent)
                        .forEach(v -> {
                            v.setCurrent(false);
                            v.setVerificationStatus(VerificationStatus.PAST);
                        });
            }

            user.getCompanyVerifications().add(UserCompanyVerification.builder()
                    .user(user)
                    .company(cmd.company())
                    .role(cmd.role())
                    .startYear(cmd.startYear())
                    .endYear(cmd.endYear())
                    .current(cmd.current())
                    .verificationStatus(cmd.current() ? VerificationStatus.CURRENT : VerificationStatus.PAST)
                    .verified(false) // Manual entry is unverified by default
                    .build());
        }
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

    private static Optional<String> inferCategory(String skill) {
        String normalized = skill == null ? "" : skill.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return Optional.empty();
        if (List.of("java", "python", "spring boot", "spring mvc", "jpa", "hibernate", "rest apis", "rest", "sql").contains(normalized)) {
            return Optional.of("backend");
        }
        if (List.of("spring security", "jwt", "rbac").contains(normalized)) {
            return Optional.of("security");
        }
        if (List.of("postgresql", "mysql", "redis", "mongodb").contains(normalized)) {
            return Optional.of("database");
        }
        if (List.of("docker", "aws", "aws ec2", "flyway", "maven", "git").contains(normalized)) {
            return Optional.of("devops");
        }
        return Optional.empty();
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
