package com.Reffr_Backend.module.referral.entity;

public enum ReferralStatus {
    /** Volunteer offered / applicant applied — awaiting decision. */
    PENDING,

    /** Seeker/referrer accepted — chat opened. */
    ACCEPTED,

    /** Explicitly rejected by the decision-maker. */
    REJECTED,

    /** Requester withdrew their own application / volunteer offer. */
    WITHDRAWN,

    /**
     * Scheduler moved PENDING referral here after expiry window passed.
     * Not a user action.
     */
    EXPIRED,

    /**
     * Another volunteer/applicant was accepted on the same post, but this
     * entry is kept as a backup. Seeker can promote an ON_HOLD volunteer to
     * ACCEPTED if the first referrer ghosts.
     */
    ON_HOLD
}