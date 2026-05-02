package com.Reffr_Backend.module.referral.dto;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.user.dto.UserDto;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReferralRequestDto {

    // ── Volunteer on REQUEST post ──────────────────────────────────────────

    @Getter
    @Setter
    public static class CreateRequest {
        /**
         * Short optional note from the volunteer.
         * Not required — a volunteer can offer without a message.
         */
        @Size(max = 300, message = "Note must be 300 characters or fewer")
        private String content;
    }

    // ── Apply to OFFER post ────────────────────────────────────────────────

    @Getter
    @Setter
    public static class ApplyRequest {
        /**
         * Optional override github link for this application.
         * If null, the referrer will see what's on the user's profile.
         */
        @Size(max = 500)
        private String githubLink;

        @Size(max = 500)
        private String linkedinLink;

        @Size(max = 300, message = "Cover note must be 300 characters or fewer")
        private String note;
    }

    // ── Response ──────────────────────────────────────────────────────────

    @Getter
    @Builder
    public static class Response {

        private UUID   id;
        private UUID   postId;

        private UserDto.UserSummary requester;
        private UserDto.UserSummary referrer;

        private ReferralStatus status;

        /** Note from the volunteer (REQUEST posts). */
        private String volunteerNote;

        /** Generic message / cover note. */
        private String content;

        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public static Response from(ReferralRequest r) {
            return Response.builder()
                    .id(r.getId())
                    .postId(r.getPost().getId())
                    .requester(UserDto.UserSummary.from(r.getRequester()))
                    .referrer(UserDto.UserSummary.from(r.getReferrer()))
                    .status(r.getStatus())
                    .volunteerNote(r.getVolunteerNote())
                    .content(r.getContent())
                    .createdAt(r.getCreatedAt())
                    .expiresAt(r.getExpiresAt())
                    .build();
        }
    }
}
