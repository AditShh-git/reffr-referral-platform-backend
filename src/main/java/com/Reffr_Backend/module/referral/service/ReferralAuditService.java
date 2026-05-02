package com.Reffr_Backend.module.referral.service;

import com.Reffr_Backend.module.referral.entity.ReferralAuditLog;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Responsible for writing to the referral audit log.
 * Isolated so it can be swapped for an event-bus approach later without
 * touching ReferralServiceImpl.
 */
public interface ReferralAuditService {

    /** Log a manual state transition triggered by a user. */
    void log(ReferralRequest referral,
             User actor,
             String action,
             ReferralStatus oldStatus,
             ReferralStatus newStatus,
             String note);

    /** Log a system-initiated transition (no human actor, e.g. scheduler). */
    void logSystem(ReferralRequest referral,
                   String action,
                   ReferralStatus oldStatus,
                   ReferralStatus newStatus,
                   String note);

    /** Fetch the full audit trail for a referral (read-only, chronological). */
    List<ReferralAuditLog> getAuditTrail(UUID referralId);
}
