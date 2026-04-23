package com.Reffr_Backend.module.referral.repository;

import com.Reffr_Backend.module.referral.entity.ReferralFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferralFeedbackRepository extends JpaRepository<ReferralFeedback, UUID> {

    boolean existsByReferralId(UUID referralId);

    Optional<ReferralFeedback> findByReferralId(UUID referralId);

    @Query("SELECT COUNT(f) FROM ReferralFeedback f WHERE f.referrer.id = :referrerId AND f.rating = 'UP'")
    long countPositive(@Param("referrerId") UUID referrerId);

    @Query("SELECT COUNT(f) FROM ReferralFeedback f WHERE f.referrer.id = :referrerId AND f.rating = 'DOWN'")
    long countNegative(@Param("referrerId") UUID referrerId);

    @Query("SELECT COUNT(f) FROM ReferralFeedback f WHERE f.referrer.id = :referrerId")
    long countAll(@Param("referrerId") UUID referrerId);
}
