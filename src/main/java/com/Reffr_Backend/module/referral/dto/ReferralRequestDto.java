package com.Reffr_Backend.module.referral.dto;

import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.user.dto.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class ReferralRequestDto {



    @Getter
    @Setter
    public static class CreateRequest {
        @NotBlank(message = "Content is required")
        @Size(min = 50, max = 500, message = "Content must be between 50 and 500 characters")
        private String content;
    }

    @Getter
    @Setter
    @Builder
    public static class Response {

        private UUID id;
        private UUID postId;

        private UserDto.UserSummary requester;
        private UserDto.UserSummary referrer;

        private ReferralStatus status;
        private String content;

        private LocalDateTime createdAt;

        public static Response from(ReferralRequest r) {
            return Response.builder()
                    .id(r.getId())
                    .postId(r.getPost().getId())
                    .requester(UserDto.UserSummary.from(r.getRequester()))
                    .referrer(UserDto.UserSummary.from(r.getReferrer()))
                    .status(r.getStatus())
                    .content(r.getContent())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

}
