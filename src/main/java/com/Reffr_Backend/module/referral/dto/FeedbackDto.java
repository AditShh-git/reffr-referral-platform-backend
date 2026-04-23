package com.Reffr_Backend.module.referral.dto;

import com.Reffr_Backend.module.referral.entity.ReferralFeedback;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

public final class FeedbackDto {

    private FeedbackDto() {}

    // ── Request ───────────────────────────────────────────────────────

    @Getter @Setter
    public static class Request {

        @NotNull(message = "Rating is required: UP or DOWN")
        private ReferralFeedback.FeedbackRating rating;

        @Size(max = 200, message = "Comment must be 200 characters or fewer")
        private String comment;
    }

    // ── Response (no sensitive info) ──────────────────────────────────

    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        private UUID                            id;
        private UUID                            referralId;
        private ReferralFeedback.FeedbackRating rating;
        private String                          comment;
        private Instant                         createdAt;

        public static Response from(ReferralFeedback f) {
            return Response.builder()
                    .id(f.getId())
                    .referralId(f.getReferral().getId())
                    .rating(f.getRating())
                    .comment(f.getComment())
                    .createdAt(f.getCreatedAt())
                    .build();
        }
    }

    // ── Aggregated reputation (shown on public profiles) ──────────────

    @Getter @Builder
    public static class ReputationSummary {
        private int    totalFeedbackCount;
        private int    positiveFeedbackCount;
        private int    negativeFeedbackCount;
        private double positiveRate;   // 0.0 – 100.0

        public static ReputationSummary from(
                long total, long positive, long negative) {
            double rate = total == 0 ? 0.0 : (double) positive / total * 100.0;
            return ReputationSummary.builder()
                    .totalFeedbackCount((int) total)
                    .positiveFeedbackCount((int) positive)
                    .negativeFeedbackCount((int) negative)
                    .positiveRate(Math.round(rate * 10.0) / 10.0)   // 1 decimal
                    .build();
        }
    }
}
