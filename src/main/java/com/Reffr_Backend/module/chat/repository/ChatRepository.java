package com.Reffr_Backend.module.chat.repository;

import com.Reffr_Backend.module.chat.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    //  FIX: Use EntityGraph instead of JOIN FETCH
    @EntityGraph(attributePaths = {"seeker", "referrer", "referral"})
    @Query("""
    SELECT c FROM Chat c
    WHERE (c.seeker.id = :userId OR c.referrer.id = :userId)
      AND c.active = true
      AND (:lastMessageAt IS NULL OR c.lastMessageAt < :lastMessageAt)
    ORDER BY c.lastMessageAt DESC NULLS LAST, c.id DESC
    """)
    Page<Chat> findByUserId(@Param("userId") UUID userId, @Param("lastMessageAt") LocalDateTime lastMessageAt, Pageable pageable);


    //  FIX: EntityGraph instead of JOIN FETCH
    @EntityGraph(attributePaths = {"seeker", "referrer", "referral"})
    @Query("""
        SELECT c FROM Chat c
        WHERE c.id = :id AND c.active = true
        """)
    Optional<Chat> findActiveById(@Param("id") UUID id);


    boolean existsByReferral_Id(UUID referralId);


    //  FIX: EntityGraph
    @EntityGraph(attributePaths = {"seeker", "referrer", "referral"})
    @Query("""
        SELECT c FROM Chat c
        WHERE c.referral.id = :referralId AND c.active = true
        """)
    Optional<Chat> findByReferralId(@Param("referralId") UUID referralId);


    @Query("""
    SELECT COUNT(m) FROM Message m
    JOIN m.chat c
    WHERE (c.seeker.id = :userId OR c.referrer.id = :userId)
      AND c.active = true
      AND m.sender.id != :userId
      AND m.read = false
    """)
    long countUnreadMessages(@Param("userId") UUID userId);
}
