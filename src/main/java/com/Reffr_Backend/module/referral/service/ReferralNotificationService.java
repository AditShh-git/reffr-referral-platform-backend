package com.Reffr_Backend.module.referral.service;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.user.entity.User;

/**
 * Owns all outbound notifications triggered by referral lifecycle events.
 * Isolated from ReferralServiceImpl so notification logic doesn't pollute
 * the orchestration layer, and can be swapped to async/queue-based delivery later.
 */
public interface ReferralNotificationService {

    /** Notify post author (seeker) that someone volunteered to refer them. */
    void notifyVolunteerReceived(ReferralRequest referral, User volunteer);

    /** Notify referrer (OFFER post author) that someone has applied. */
    void notifyApplicantReceived(ReferralRequest referral, User applicant);

    /** Notify the requester that their referral was accepted. */
    void notifyAccepted(ReferralRequest referral);

    /** Notify the requester that their referral was rejected. */
    void notifyRejected(ReferralRequest referral);

    /** Notify the referrer that the requester withdrew. */
    void notifyWithdrawn(ReferralRequest referral);
}
