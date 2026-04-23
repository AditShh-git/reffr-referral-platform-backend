package com.Reffr_Backend.module.feed.repository;

import com.Reffr_Backend.module.feed.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    // ── Feed ──────────────────────────────────────────────────────────

    /** All active, non-expired posts — latest first */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.active = true
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:lastCreatedAt IS NULL OR p.createdAt < :lastCreatedAt)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Page<Post> findActiveFeed(@Param("lastCreatedAt") Instant lastCreatedAt, Pageable pageable);

    /** Active posts filtered by type (REQUEST or OFFER) — latest first */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.active = true
          AND p.type = :type
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:lastCreatedAt IS NULL OR p.createdAt < :lastCreatedAt)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Page<Post> findActiveByType(
            @Param("type") Post.PostType type,
            @Param("lastCreatedAt") Instant lastCreatedAt,
            Pageable pageable);

    /** Posts by a specific author — latest first (profile timeline) */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.author.id = :authorId
          AND p.active = true
          AND (:lastCreatedAt IS NULL OR p.createdAt < :lastCreatedAt)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Page<Post> findByAuthorId(
            @Param("authorId") UUID authorId,
            @Param("lastCreatedAt") Instant lastCreatedAt,
            Pageable pageable);

    /** Personalized Feed based on matching skills ranking */
    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
    SELECT p FROM Post p
    JOIN p.author a
    WHERE p.active = true
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
    ORDER BY
      CASE WHEN p.type = 'OFFER' THEN 0 ELSE 1 END ASC,
      CASE WHEN EXISTS (
          SELECT 1 FROM UserCompanyVerification v 
          WHERE v.user = a 
            AND v.verified = true 
            AND v.verificationStatus = 'CURRENT'
      ) THEN 0 ELSE 1 END ASC,
      CASE WHEN EXISTS (
          SELECT 1 FROM PostTag t 
          WHERE t.post = p AND t.tag IN :skills
      ) THEN 0 ELSE 1 END ASC,
      p.createdAt DESC
    """)
    Page<Post> findPersonalizedFeed(@Param("skills") List<String> skills, Pageable pageable);

    /** Fallback Default Feed for users with no skills */
    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
    SELECT DISTINCT p FROM Post p
    JOIN p.author a
    WHERE p.active = true
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
    ORDER BY 
      CASE WHEN p.type = 'OFFER' THEN 0 ELSE 1 END ASC,
      CASE WHEN EXISTS (
          SELECT 1 FROM UserCompanyVerification v
          WHERE v.user = a
            AND v.verified = true
            AND v.verificationStatus = 'CURRENT'
      ) THEN 0 ELSE 1 END ASC,
      p.createdAt DESC
    """)
    Page<Post> findDefaultFeed(Pageable pageable);

    // ── Search ────────────────────────────────────────────────────────

    /** Cross-type search with optional company / role / experience filters */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.active = true
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:lastCreatedAt IS NULL OR p.createdAt < :lastCreatedAt)
          AND (:company IS NULL OR LOWER(p.company) LIKE LOWER(CONCAT('%', :company, '%')))
          AND (:role    IS NULL OR LOWER(p.currentRole)    LIKE LOWER(CONCAT('%', :role,    '%')))
          AND (:minExp  IS NULL OR (p.minExperience IS NULL OR p.minExperience >= :minExp))
          AND (:maxExp  IS NULL OR (p.maxExperience IS NULL OR p.maxExperience <= :maxExp))
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Page<Post> search(
            @Param("company") String  company,
            @Param("role")    String  role,
            @Param("minExp")  Integer minExp,
            @Param("maxExp")  Integer maxExp,
            @Param("lastCreatedAt") Instant lastCreatedAt,
            Pageable pageable);

    /** Typed search — sorted by latest */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.active = true
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:lastCreatedAt IS NULL OR p.createdAt < :lastCreatedAt)
          AND (:type    IS NULL OR p.type    = :type)
          AND (:company IS NULL OR LOWER(p.company) LIKE LOWER(CONCAT('%', :company, '%')))
          AND (:role    IS NULL OR LOWER(p.currentRole)    LIKE LOWER(CONCAT('%', :role,    '%')))
          AND (:minExp  IS NULL OR (p.minExperience IS NULL OR p.minExperience >= :minExp))
          AND (:maxExp  IS NULL OR (p.maxExperience IS NULL OR p.maxExperience <= :maxExp))
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    Page<Post> searchByTypeLatest(
            @Param("type")    Post.PostType type,
            @Param("company") String        company,
            @Param("role")    String        role,
            @Param("minExp")  Integer       minExp,
            @Param("maxExp")  Integer       maxExp,
            @Param("lastCreatedAt") Instant lastCreatedAt,
            Pageable pageable);

    /** Typed search — sorted by trending (views + referrals weighted) */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.active = true
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:type    IS NULL OR p.type    = :type)
          AND (:company IS NULL OR LOWER(p.company) LIKE LOWER(CONCAT('%', :company, '%')))
          AND (:role    IS NULL OR LOWER(p.currentRole)    LIKE LOWER(CONCAT('%', :role,    '%')))
          AND (:minExp  IS NULL OR (p.minExperience IS NULL OR p.minExperience >= :minExp))
          AND (:maxExp  IS NULL OR (p.maxExperience IS NULL OR p.maxExperience <= :maxExp))
        ORDER BY (p.viewsCount + p.referralCount * 3) DESC, p.createdAt DESC, p.id DESC
        """)
    Page<Post> searchByTypeTrending(
            @Param("type")    Post.PostType type,
            @Param("company") String        company,
            @Param("role")    String        role,
            @Param("minExp")  Integer       minExp,
            @Param("maxExp")  Integer       maxExp,
            Pageable pageable);

    // ── Single post ───────────────────────────────────────────────────

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.author
        WHERE p.id = :id AND p.active = true
        """)
    Optional<Post> findActiveById(@Param("id") UUID id);

    // ── Mutations ─────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Post p SET p.viewsCount    = p.viewsCount    + 1 WHERE p.id = :id")
    void incrementViews(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.referralCount = p.referralCount + 1 WHERE p.id = :id")
    void incrementReferralCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.active = false WHERE p.id = :id")
    void softDelete(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.active = false WHERE p.active = true AND p.expiresAt < CURRENT_TIMESTAMP")
    int deactivateExpiredPosts();

    // ── Utilities ─────────────────────────────────────────────────────

    long countByAuthorIdAndActive(UUID authorId, boolean active);

    @Query("SELECT p FROM Post p JOIN FETCH p.tags WHERE p.id IN :ids")
    List<Post> findWithTagsByIds(@Param("ids") List<UUID> ids);
}