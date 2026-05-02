package com.Reffr_Backend.module.referral.service.impl;

import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.service.EmailService;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.service.ReferralNotificationService;
import com.Reffr_Backend.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sends all in-app notifications and emails triggered by referral lifecycle events.
 * Called from ReferralServiceImpl after state transitions are committed.
 *
 * Keeping this isolated means notification delivery can be made async (e.g. via
 * @Async or a message queue) without touching the core orchestration logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralNotificationServiceImpl implements ReferralNotificationService {

    private final NotificationService notificationService;
    private final EmailService        emailService;

    @Override
    public void notifyVolunteerReceived(ReferralRequest referral, User volunteer) {
        // Seeker (post author = requester) is notified
        notificationService.send(
                referral.getRequester().getId(),
                NotificationType.REFERRAL_VOLUNTEER,
                NotificationMessages.referralVolunteerTitle(),
                NotificationMessages.referralVolunteerBody(volunteer.getName()),
                "REFERRAL",
                referral.getId().toString()
        );
        log.debug("Notification(VOLUNTEER_RECEIVED) → seekerId={}", referral.getRequester().getId());
    }

    @Override
    public void notifyApplicantReceived(ReferralRequest referral, User applicant) {
        // Referrer (OFFER post author) is notified
        notificationService.send(
                referral.getReferrer().getId(),
                NotificationType.REFERRAL_REQUEST_RECEIVED,
                NotificationMessages.referralRequestTitle(),
                NotificationMessages.referralRequestBody(
                        referral.getPost().getCurrentRole(),
                        referral.getPost().getCompany()),
                "REFERRAL",
                referral.getId().toString()
        );
        log.debug("Notification(APPLICANT_RECEIVED) → referrerId={}", referral.getReferrer().getId());
    }

    @Override
    public void notifyAccepted(ReferralRequest referral) {
        User requester = referral.getRequester();
        User referrer  = referral.getReferrer();

        notificationService.send(
                requester.getId(),
                NotificationType.REFERRAL_ACCEPTED,
                NotificationMessages.referralAcceptedTitle(),
                NotificationMessages.referralAcceptedBody(referrer.getName()),
                "REFERRAL",
                referral.getId().toString()
        );

        if (requester.getPrimaryEmail() != null) {
            // Pass null chatId — chat UUID lookup can be done by referralId on frontend
            emailService.sendReferralAccepted(requester, referrer.getName(), (java.util.UUID) null);
        }
        log.debug("Notification(ACCEPTED) → requesterId={}", requester.getId());
    }

    @Override
    public void notifyRejected(ReferralRequest referral) {
        User requester = referral.getRequester();
        User referrer  = referral.getReferrer();

        notificationService.send(
                requester.getId(),
                NotificationType.REFERRAL_REJECTED,
                NotificationMessages.referralRejectedTitle(),
                NotificationMessages.referralRejectedBody(referrer.getName()),
                "REFERRAL",
                referral.getId().toString()
        );

        if (requester.getPrimaryEmail() != null) {
            emailService.sendReferralRejected(requester, referrer.getName());
        }
        log.debug("Notification(REJECTED) → requesterId={}", requester.getId());
    }

    @Override
    public void notifyWithdrawn(ReferralRequest referral) {
        notificationService.send(
                referral.getReferrer().getId(),
                NotificationType.REFERRAL_WITHDRAWN,
                NotificationMessages.referralWithdrawnTitle(),
                NotificationMessages.referralWithdrawnBody(referral.getRequester().getName()),
                "REFERRAL",
                referral.getId().toString()
        );
        log.debug("Notification(WITHDRAWN) → referrerId={}", referral.getReferrer().getId());
    }
}
