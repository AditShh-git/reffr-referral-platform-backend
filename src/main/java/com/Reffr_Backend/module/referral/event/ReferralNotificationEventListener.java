package com.Reffr_Backend.module.referral.event;

import com.Reffr_Backend.module.referral.service.ReferralNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Fires referral notifications <em>after</em> the originating DB transaction has
 * successfully committed, guaranteeing that:
 * <ol>
 *   <li>The referral row is fully visible to any recipient's subsequent read.</li>
 *   <li>No notification is sent for a transaction that was later rolled back.</li>
 * </ol>
 *
 * <p>Bound to {@link TransactionPhase#AFTER_COMMIT} — the listener is a no-op if
 * the transaction rolls back or is never committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralNotificationEventListener {

    private final ReferralNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReferralLifecycleEvent(ReferralLifecycleEvent event) {
        try {
            switch (event.getAction()) {
                case VOLUNTEER_RECEIVED ->
                        notificationService.notifyVolunteerReceived(event.getReferral(), event.getActor());
                case APPLICANT_RECEIVED ->
                        notificationService.notifyApplicantReceived(event.getReferral(), event.getActor());
                case ACCEPTED  -> notificationService.notifyAccepted(event.getReferral());
                case REJECTED  -> notificationService.notifyRejected(event.getReferral());
                case WITHDRAWN -> notificationService.notifyWithdrawn(event.getReferral());
            }
        } catch (Exception ex) {
            // Notification failure must NOT roll back or mask the committed referral state.
            log.error("Notification delivery failed for referralId={} action={}: {}",
                    event.getReferral().getId(), event.getAction(), ex.getMessage(), ex);
        }
    }
}
