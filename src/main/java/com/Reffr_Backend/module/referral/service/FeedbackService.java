package com.Reffr_Backend.module.referral.service;

import com.Reffr_Backend.module.referral.dto.FeedbackDto;

import java.util.UUID;

public interface FeedbackService {

    /** Seeker submits UP/DOWN feedback after referral has been marked REFERRED. */
    FeedbackDto.Response submitFeedback(UUID referralId, UUID seekerId, FeedbackDto.Request request);

    /** Aggregated public reputation for a referrer — no individual details exposed. */
    FeedbackDto.ReputationSummary getReputation(UUID referrerId);
}
