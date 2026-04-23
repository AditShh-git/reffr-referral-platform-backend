package com.Reffr_Backend.module.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.Reffr_Backend.module.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;
import java.io.Serializable;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserDto {

    // ── Onboarding Request ──────────────────────────────────────────────
    @Getter @Setter
    public static class OnboardingRequest {
        @NotBlank(message = "Role is required")
        @Size(max = 150)
        private String currentRole;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^(?!.*noreply\\.github\\.com).*$", message = "GitHub noreply emails are not allowed")
        private String email;

        @NotNull(message = "Skills cannot be null")
        @Size(min = 3, message = "You must provide at least 3 skills")
        private List<@NotBlank String> skills;

        @NotNull(message = "Experience is required")
        @Min(value = 0, message = "Experience cannot be negative")
        private Integer yearsOfExperience;

        @NotBlank(message = "Resume URL is required")
        private String resumeUrl;
    }


    private UserDto() {}

    // ── Profile Update Request ──────────────────────────────────────────
    @Getter @Setter
    public static class UpdateProfileRequest {

        @Size(max = 150, message = "Name must be at most 150 characters")
        private String name;

        @Size(max = 500, message = "Bio must be at most 500 characters")
        private String bio;

        @Size(max = 150, message = "Location must be at most 150 characters")
        private String location;

        @Size(max = 150, message = "Company name must be at most 150 characters")
        private String currentCompany;

        @Size(max = 150, message = "Role must be at most 150 characters")
        private String currentRole;

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 50, message = "Years of experience seems too high")
        private Short yearsOfExperience;

        @Email(message = "Invalid email format")
        @Pattern(regexp = "^(?!.*noreply\\.github\\.com).*$", message = "GitHub noreply emails are not allowed")
        private String email;

        @Size(max = 20, message = "Maximum 20 skills allowed")
        private List<@Size(max = 100) String> skills;

        private List<ExperienceRequest> experiences;
    }

    // ── Experience / Verification Request ──
    @Getter @Setter
    public static class ExperienceRequest {
        @NotBlank(message = "Company is required")
        @Size(max = 150)
        private String company;

        @NotBlank(message = "Role is required")
        @Size(max = 150)
        private String role;

        private Short startYear;
        private Short endYear;
        private boolean current;
    }

    @Getter @Setter
    public static class PublicProofRequest {
        @NotBlank(message = "Company is required")
        private String company;
        @NotBlank(message = "Profile URL is required")
        private String profileUrl;
    }

    @Getter @Setter
    public static class DocumentProofRequest {
        @NotBlank(message = "Company is required")
        private String company;
        @NotBlank(message = "Document key is required")
        private String documentKey;
    }

    // ── Full Profile Response (own profile) ────────────────────────────
    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileResponse {
        private UUID   id;
        private String githubUsername;
        private String githubUrl;
        private String name;
        private String primaryEmail;
        private String avatarUrl;
        private String bio;
        private String location;
        private String currentCompany;
        private String currentRole;
        private Short  yearsOfExperience;
        private List<String>             skills;

        // Resume
        private boolean hasResume;
        private String  resumeOriginalName;
        private Instant resumeUploadedAt;

        // Company verification & History
        private List<VerificationResponse> companyVerifications;

        // Reputation (feedback-based — no individual outcome details)
        private Integer totalReferralsGiven;
        private Integer positiveFeedbackCount;
        private Integer negativeFeedbackCount;
        private Double  referralSuccessRate;

        private Instant createdAt;
        private Instant lastSeenAt;

        private boolean onboardingCompleted;

        public static ProfileResponse from(User user) {
            return ProfileResponse.builder()
                    .id(user.getId())
                    .githubUsername(user.getGithubUsername())
                    .githubUrl(user.getGithubUrl())
                    .name(user.getName())
                    .primaryEmail(user.getPrimaryEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .location(user.getLocation())
                    .currentCompany(user.getCurrentCompany())
                    .currentRole(user.getCurrentRole())
                    .skills(user.getSkills().stream().map(s -> s.getSkill()).toList())
                    .companyVerifications(user.getCompanyVerifications().stream()
                            .map(VerificationResponse::from).toList())
                    .hasResume(user.hasResume())
                    .resumeOriginalName(user.getResumeOriginalName())
                    .resumeUploadedAt(user.getResumeUploadedAt())
                    .totalReferralsGiven(user.getTotalReferralsGiven())
                    .positiveFeedbackCount(user.getPositiveFeedbackCount())
                    .negativeFeedbackCount(user.getNegativeFeedbackCount())
                    .referralSuccessRate(user.getReferralSuccessRate())
                    .createdAt(user.getCreatedAt())
                    .lastSeenAt(user.getLastSeenAt())
                    .onboardingCompleted(user.isOnboardingCompleted())
                    .build();
        }
    }

    // ── Public Profile Response (viewed by others) ─────────────────────
    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicProfileResponse {
        private UUID   id;
        private String githubUsername;
        private String githubUrl;
        private String name;
        private String avatarUrl;
        private String bio;
        private String location;
        private String currentCompany;
        private String currentRole;
        private Short  yearsOfExperience;
        private List<String>             skills;

        // Resume (URL - pre-signed from S3)
        private boolean hasResume;
        private String  resumeUrl;

        private List<VerificationResponse> companyVerifications;

        // Reputation (aggregated only — no individual outcomes)
        private Integer totalReferralsGiven;
        private Integer positiveFeedbackCount;
        private Integer negativeFeedbackCount;
        private Double  referralSuccessRate;

        public static PublicProfileResponse from(User user, String resumeUrl) {
            return PublicProfileResponse.builder()
                    .id(user.getId())
                    .githubUsername(user.getGithubUsername())
                    .githubUrl(user.getGithubUrl())
                    .name(user.getName())
                    .avatarUrl(user.getAvatarUrl())
                    .bio(user.getBio())
                    .location(user.getLocation())
                    .currentCompany(user.getCurrentCompany())
                    .currentRole(user.getCurrentRole())
                    .skills(user.getSkills().stream().map(s -> s.getSkill()).toList())
                    .companyVerifications(user.getCompanyVerifications().stream()
                            .map(VerificationResponse::from).toList())
                    .hasResume(user.hasResume())
                    .resumeUrl(resumeUrl)
                    .totalReferralsGiven(user.getTotalReferralsGiven())
                    .positiveFeedbackCount(user.getPositiveFeedbackCount())
                    .negativeFeedbackCount(user.getNegativeFeedbackCount())
                    .referralSuccessRate(user.getReferralSuccessRate())
                    .build();
        }
    }

    // ── Verification response (History) ──
    @Getter @Builder
    public static class VerificationResponse {
        private UUID    id;
        private String  company;
        private String  role;
        private Short   startYear;
        private Short   endYear;
        private boolean current;
        private String  status;
        private boolean verified;
        private String  proofType;
        private Instant verifiedAt;

        public static VerificationResponse from(
                com.Reffr_Backend.module.user.entity.UserCompanyVerification v) {
            return VerificationResponse.builder()
                    .id(v.getId())
                    .company(v.getCompany())
                    .role(v.getRole())
                    .startYear(v.getStartYear())
                    .endYear(v.getEndYear())
                    .current(v.isCurrent())
                    .status(v.getVerificationStatus().name())
                    .verified(v.isVerified())
                    .proofType(v.getProofType() != null ? v.getProofType().name() : null)
                    .verifiedAt(v.getVerifiedAt())
                    .build();
        }
    }


    // ── Summary (used in feeds, referral cards) ────────────────────────
    @Getter @Builder
    public static class UserSummary implements Serializable {
        private UUID    id;
        private String  githubUsername;
        private String  name;
        private String  avatarUrl;
        private String  currentCompany;
        private String  currentRole;
        private Double  referralSuccessRate;

        public static UserSummary from(User user) {
            return UserSummary.builder()
                    .id(user.getId())
                    .githubUsername(user.getGithubUsername())
                    .name(user.getName())
                    .avatarUrl(user.getAvatarUrl())
                    .currentCompany(user.getCurrentCompany())
                    .currentRole(user.getCurrentRole())
                    .referralSuccessRate(user.getReferralSuccessRate())
                    .build();
        }
    }
}
