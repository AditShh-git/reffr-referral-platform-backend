package com.Reffr_Backend.module.referral.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.referral.dto.FeedbackDto;
import com.Reffr_Backend.module.referral.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Referral Feedback & Reputation")
@SecurityRequirement(name = "bearerAuth")
@RequiresOnboarding
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/{referralId}/feedback")
    @Operation(
        summary = "Submit referral feedback",
        description = "Seeker submits UP/DOWN feedback after the referral has been submitted (REFERRED stage). One feedback per referral."
    )
    public ResponseEntity<ApiResponse<FeedbackDto.Response>> submitFeedback(
            @PathVariable UUID referralId,
            @Valid @RequestBody FeedbackDto.Request request) {

        FeedbackDto.Response response = feedbackService.submitFeedback(
                referralId, SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(201).body(ApiResponse.success("Feedback submitted", response));
    }

    @GetMapping("/reputation/{referrerId}")
    @Operation(
        summary = "Get referrer reputation",
        description = "Returns aggregated UP/DOWN counts and success rate. No individual feedback or outcome details are exposed."
    )
    public ResponseEntity<ApiResponse<FeedbackDto.ReputationSummary>> getReputation(
            @PathVariable UUID referrerId) {

        FeedbackDto.ReputationSummary summary = feedbackService.getReputation(referrerId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
