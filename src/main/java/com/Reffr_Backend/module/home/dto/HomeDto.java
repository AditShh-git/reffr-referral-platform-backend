package com.Reffr_Backend.module.home.dto;

import com.Reffr_Backend.module.user.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class HomeDto {

    private HomeDto() {}

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopReferrerResponse implements Serializable {
        private UUID id;
        private String name;
        private String currentCompany;
        private Integer successfulReferrals;
        private String avatarUrl;

        public static TopReferrerResponse from(com.Reffr_Backend.module.user.entity.User user) {
            return TopReferrerResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .currentCompany(user.getCurrentCompany())
                    .successfulReferrals(user.getSuccessfulReferrals())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        }
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompanyStatsResponse implements Serializable {
        private String companyName;
        private String logoUrl;
        private Long openReferralCount;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SkillStatsResponse implements Serializable {
        private String skillName;
        private Long frequencyCount;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActivityResponse implements Serializable {
        private String type; // e.g., "NEW_POST", "REFERRAL_UPDATE", "NEW_MESSAGE", "NEW_FOLLOWER"
        private String message;
        private Instant createdAt;
        private String entityId;
        private String entityType;
    }
}
