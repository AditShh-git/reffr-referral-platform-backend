package com.Reffr_Backend.module.user.dto;

import com.Reffr_Backend.module.user.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserDto {

    @Getter
    @Setter
    public static class OnboardingRequest {
        @Size(max = 150, message = "Name must be at most 150 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^(?!.*noreply\\.github\\.com).*$", message = "GitHub noreply emails are not allowed")
        private String email;

        @NotNull(message = "Skills cannot be null")
        @Size(min = 3, message = "You must provide at least 3 skills")
        private List<@NotBlank String> skills;

        @NotBlank(message = "Role is required")
        @Size(max = 150)
        private String currentRole;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User onboarding and profile completeness status")
    public static class OnboardingStatusResponse {
        @Schema(description = "Whether basic profile details (name, email, role) are completed", example = "true")
        private boolean profileCompleted;

        @Schema(description = "Whether the user has uploaded a resume", example = "true")
        private boolean resumeUploaded;

        @Schema(description = "Whether the resume has been successfully parsed into skills", example = "true")
        private boolean skillsParsed;
    }

    private UserDto() {}

    @Getter
    @Setter
    public static class UpdateEmailRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(regexp = "^(?!.*noreply\\.github\\.com).*$", message = "GitHub noreply emails are not allowed")
        private String email;
    }

    @Getter
    public static class UpdateProfileDetailsRequest {
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
        @Setter
        private Short yearsOfExperience;

        public void setName(String name) { this.name = name != null ? name.trim() : null; }
        public void setBio(String bio) { this.bio = bio != null ? bio.trim() : null; }
        public void setLocation(String location) { this.location = location != null ? location.trim() : null; }
        public void setCurrentCompany(String currentCompany) { this.currentCompany = currentCompany != null ? currentCompany.trim() : null; }
        public void setCurrentRole(String currentRole) { this.currentRole = currentRole != null ? currentRole.trim() : null; }
    }

    @Getter
    @Setter
    @Deprecated
    public static class UpdateProfileRequest {
        private String name;
        private String bio;
        private String location;
        private String currentCompany;
        private String currentRole;
        private Short yearsOfExperience;
        private String email;
        private List<String> skills;
        private List<ExperienceRequest> experiences;
    }

    @Getter
    @Setter
    public static class UpdateSkillsRequest {
        @NotNull(message = "Skills cannot be null")
        @Size(max = 20, message = "Maximum 20 skills allowed")
        private List<@NotBlank @Size(max = 100) String> skills;
    }

    @Getter
    @Setter
    public static class ExperienceRequest {
        @NotBlank(message = "Company is required")
        @Size(max = 150)
        private String company;

        @NotBlank(message = "Role is required")
        @Size(max = 150)
        private String role;

        @Min(value = 1900, message = "Start year is too early")
        @Max(value = 2100, message = "Start year is too late")
        private Short startYear;

        @Min(value = 1900, message = "End year is too early")
        @Max(value = 2100, message = "End year is too late")
        private Short endYear;

        private boolean current;
    }

    @Getter
    @Setter
    public static class PublicProofRequest {
        @NotBlank(message = "Company is required")
        private String company;

        @NotBlank(message = "Profile URL is required")
        private String profileUrl;
    }

    @Getter
    @Setter
    public static class DocumentProofRequest {
        @NotBlank(message = "Company is required")
        private String company;

        @NotBlank(message = "Document key is required")
        private String documentKey;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileResponse {
        private UUID id;
        private String githubUsername;
        private String githubUrl;
        private String name;
        private String primaryEmail;
        private String avatarUrl;
        private String bio;
        private String location;
        private String currentCompany;
        private String currentRole;
        private Short yearsOfExperience;
        private List<String> skills;
        private boolean hasResume;
        private String resumeOriginalName;
        private Instant resumeUploadedAt;
        private List<VerificationResponse> companyVerifications;
        private Integer totalReferralsGiven;
        private Integer positiveFeedbackCount;
        private Integer negativeFeedbackCount;
        private Double referralSuccessRate;
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
                    .skills(user.getSkills().stream().map(s -> s.getSkillName()).toList())
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParsedResumeResponse {
        public enum ParsingStatus { PENDING, READY, NOT_FOUND, FAILED, WEAK_DATA }
        public enum ConfidenceLevel { HIGH, MEDIUM, LOW }

        private Integer version;
        private ParsingStatus status;
        private List<ParsedSkill> skills;
        private String role;
        private ConfidenceLevel confidence;
        private String error;
        private Instant parsedAt;
        private String resumeKey;

        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ParsedSkill {
            private String name;
            private String category;
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublicProfileResponse {
        private UUID id;
        private String githubUsername;
        private String githubUrl;
        private String name;
        private String avatarUrl;
        private String bio;
        private String location;
        private String currentCompany;
        private String currentRole;
        private Short yearsOfExperience;
        private List<String> skills;
        private boolean hasResume;
        private String resumeUrl;
        private List<VerificationResponse> companyVerifications;
        private Integer totalReferralsGiven;
        private Integer positiveFeedbackCount;
        private Integer negativeFeedbackCount;
        private Double referralSuccessRate;

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
                    .skills(user.getSkills().stream().map(s -> s.getSkillName()).toList())
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

    @Getter
    @Builder
    public static class VerificationResponse {
        private UUID id;
        private String company;
        private String role;
        private Short startYear;
        private Short endYear;
        private boolean current;
        private String status;
        private boolean verified;
        private String proofType;
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

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileViewItem {
        private UUID id;
        private String githubUsername;
        private String name;
        private String avatarUrl;
        private String currentCompany;
        private String currentRole;
        private Instant viewedAt;

        public static ProfileViewItem from(User user, Instant viewedAt) {
            return ProfileViewItem.builder()
                    .id(user.getId())
                    .githubUsername(user.getGithubUsername())
                    .name(user.getName())
                    .avatarUrl(user.getAvatarUrl())
                    .currentCompany(user.getCurrentCompany())
                    .currentRole(user.getCurrentRole())
                    .viewedAt(viewedAt)
                    .build();
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileViewHistoryResponse {
        private List<ProfileViewItem> viewers;
        private boolean limited;
        private int limit;
        private long totalViews;
    }

    @Getter
    @Builder
    public static class UserSummary implements Serializable {
        private UUID id;
        private String githubUsername;
        private String name;
        private String avatarUrl;
        private String currentCompany;
        private String currentRole;
        private Double referralSuccessRate;

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
