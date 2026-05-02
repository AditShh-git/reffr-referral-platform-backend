package com.Reffr_Backend.module.referral.scheduler;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.referral.service.ReferralAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that transitions stale PENDING referrals to EXPIRED.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Fetch the referral records to expire (for audit logging per record)</li>
 *   <li>Bulk-update their status to EXPIRED</li>
 *   <li>Write an audit log entry for each (actor = null = system event)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralExpiryScheduler {

    private final ReferralRequestRepository referralRepository;
    private final ReferralAuditService      auditService;

    @Scheduled(cron = "0 0 * * * *")  // every hour on the hour
    @Transactional
    public void expireStaleReferrals() {
        LocalDateTime cutoff = LocalDateTime.now();

        // Fetch BEFORE bulk-update so we can audit each record individually
        List<ReferralRequest> toExpire = referralRepository.findExpiredPending(cutoff);

        if (toExpire.isEmpty()) {
            log.debug("ReferralExpiryScheduler: nothing to expire");
            return;
        }

        // Bulk status update
        int count = referralRepository.expirePendingOlderThan(cutoff);

        // Audit each as a system event (actor = null)
        toExpire.forEach(r -> auditService.logSystem(
                r, "EXPIRED",
                ReferralStatus.PENDING,
                ReferralStatus.EXPIRED,
                "Auto-expired when expiresAt elapsed"
        ));

        log.info("ReferralExpiryScheduler: expired {} referrals (cutoff={})", count, cutoff);
    }
}
