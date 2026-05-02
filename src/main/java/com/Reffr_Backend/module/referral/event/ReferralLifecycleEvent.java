package com.Reffr_Backend.module.referral.event;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.user.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Domain event published inside a {@code @Transactional} method after a referral
 * state transition has been persisted (and flushed where necessary).
 *
 * <p>Consumed by {@link ReferralNotificationEventListener} which is bound to
 * {@code TransactionPhase.AFTER_COMMIT}, ensuring notifications are only sent
 * once the DB transaction has successfully committed.
 */
@Getter
public class ReferralLifecycleEvent extends ApplicationEvent {

    public enum Action {
        VOLUNTEER_RECEIVED,   // REQUEST post: a volunteer just offered to refer
        APPLICANT_RECEIVED,   // OFFER post: an applicant just applied
        ACCEPTED,
        REJECTED,
        WITHDRAWN
    }

    private final Action         action;
    private final ReferralRequest referral;
    /** Non-null only for VOLUNTEER_RECEIVED and APPLICANT_RECEIVED. */
    private final User           actor;

    private ReferralLifecycleEvent(Object source, Action action, ReferralRequest referral, User actor) {
        super(source);
        this.action   = action;
        this.referral = referral;
        this.actor    = actor;
    }

    public static ReferralLifecycleEvent volunteerReceived(Object source, ReferralRequest referral, User volunteer) {
        return new ReferralLifecycleEvent(source, Action.VOLUNTEER_RECEIVED, referral, volunteer);
    }

    public static ReferralLifecycleEvent applicantReceived(Object source, ReferralRequest referral, User applicant) {
        return new ReferralLifecycleEvent(source, Action.APPLICANT_RECEIVED, referral, applicant);
    }

    public static ReferralLifecycleEvent accepted(Object source, ReferralRequest referral) {
        return new ReferralLifecycleEvent(source, Action.ACCEPTED, referral, null);
    }

    public static ReferralLifecycleEvent rejected(Object source, ReferralRequest referral) {
        return new ReferralLifecycleEvent(source, Action.REJECTED, referral, null);
    }

    public static ReferralLifecycleEvent withdrawn(Object source, ReferralRequest referral) {
        return new ReferralLifecycleEvent(source, Action.WITHDRAWN, referral, null);
    }
}
