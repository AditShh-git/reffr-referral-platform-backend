package com.Reffr_Backend.module.referral.service.impl;

import com.Reffr_Backend.module.referral.entity.ReferralAuditLog;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.module.referral.repository.ReferralAuditLogRepository;
import com.Reffr_Backend.module.referral.service.ReferralAuditService;
import com.Reffr_Backend.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralAuditServiceImpl implements ReferralAuditService {

    private final ReferralAuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(ReferralRequest referral,
                    User actor,
                    String action,
                    ReferralStatus oldStatus,
                    ReferralStatus newStatus,
                    String note) {
        ReferralAuditLog entry = ReferralAuditLog.of(referral, actor, action, oldStatus, newStatus, note);
        auditLogRepository.save(entry);
        log.debug("Audit — referralId={} action={} {}→{} actor={}",
                referral.getId(), action,
                oldStatus != null ? oldStatus : "-",
                newStatus != null ? newStatus : "-",
                actor != null ? actor.getId() : "SYSTEM");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void logSystem(ReferralRequest referral,
                          String action,
                          ReferralStatus oldStatus,
                          ReferralStatus newStatus,
                          String note) {
        log(referral, null, action, oldStatus, newStatus, note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralAuditLog> getAuditTrail(UUID referralId) {
        return auditLogRepository.findByReferralIdOrderByCreatedAt(referralId);
    }
}
