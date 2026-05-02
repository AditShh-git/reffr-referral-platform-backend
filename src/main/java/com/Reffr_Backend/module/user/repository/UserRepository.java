package com.Reffr_Backend.module.user.repository;


import com.Reffr_Backend.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGithubId(String githubId);

    Optional<User> findByGithubUsername(String githubUsername);

    Optional<User> findByPrimaryEmail(String email);

    boolean existsByGithubUsername(String githubUsername);

    boolean existsByPrimaryEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.skills LEFT JOIN FETCH u.companyVerifications WHERE u.id = :id")
    Optional<User> findByIdWithProfile(@Param("id") UUID id);

    // ── Search ──
    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND (
            LOWER(u.name)           LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.githubUsername) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.currentCompany) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(u.currentRole)    LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY u.totalReferralsGiven DESC
        """)
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

    // ── Find referrers at a specific company ──
    @Query("""
        SELECT u FROM User u
        JOIN u.companyVerifications v
        WHERE u.active = true
          AND v.verified = true
          AND v.verificationStatus = com.Reffr_Backend.module.user.entity.VerificationStatus.CURRENT
          AND LOWER(v.company) = LOWER(:company)
        ORDER BY u.successfulReferrals DESC
        """)
    Page<User> findVerifiedReferrersAtCompany(@Param("company") String company, Pageable pageable);

    // ── Update last seen ──
    @Modifying
    @Query("UPDATE User u SET u.lastSeenAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void updateLastSeen(@Param("id") UUID id);

    // ── Referral reputation update ──
    @Modifying
    @Query("""
        UPDATE User u
        SET u.totalReferralsGiven = u.totalReferralsGiven + 1
        WHERE u.id = :referrerId
        """)
    void incrementReferralsGiven(@Param("referrerId") UUID referrerId);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.successfulReferrals = u.successfulReferrals + 1
        WHERE u.id = :referrerId
        """)
    void incrementSuccessfulReferrals(@Param("referrerId") UUID referrerId);

    // ── Company match (for smart notifications) ──
    /**
     * Returns IDs of active users whose currentCompany matches the given tag.
     * Returns IDs only to avoid loading full entities for potentially large result sets.
     */
    @Query("""
        SELECT u.id FROM User u
        WHERE u.active = true
          AND LOWER(u.currentCompany) = LOWER(:company)
        """)
    java.util.List<UUID> findIdsByCurrentCompanyIgnoreCase(@Param("company") String company);

    @Query("""
        SELECT u FROM User u
        WHERE u.active = true
          AND u.successfulReferrals > 0
        ORDER BY u.successfulReferrals DESC
        """)
    java.util.List<User> findTopReferrers(Pageable pageable);
}