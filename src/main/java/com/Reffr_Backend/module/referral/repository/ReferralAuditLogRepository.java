package com.Reffr_Backend.module.referral.repository;

import com.Reffr_Backend.module.referral.entity.ReferralAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReferralAuditLogRepository extends JpaRepository<ReferralAuditLog, UUID> {

    /** Full audit trail for a referral, ordered chronologically. */
    @Query("""
        SELECT a FROM ReferralAuditLog a
        LEFT JOIN FETCH a.actor
        WHERE a.referral.id = :referralId
        ORDER BY a.createdAt ASC
    """)
    List<ReferralAuditLog> findByReferralIdOrderByCreatedAt(@Param("referralId") UUID referralId);
}
