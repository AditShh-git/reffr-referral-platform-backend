package com.Reffr_Backend.module.feed.repository;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
    SELECT p FROM Post p
    JOIN p.author a
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
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
      p.createdAt DESC,
      p.id DESC
    """)
    Page<Post> findPersonalizedFeed(@Param("skills") List<String> skills, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "tags"})
    @Query("""
    SELECT DISTINCT p FROM Post p
    JOIN p.author a
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
    ORDER BY
      CASE WHEN p.type = 'OFFER' THEN 0 ELSE 1 END ASC,
      CASE WHEN EXISTS (
          SELECT 1 FROM UserCompanyVerification v
          WHERE v.user = a
            AND v.verified = true
            AND v.verificationStatus = 'CURRENT'
      ) THEN 0 ELSE 1 END ASC,
      p.createdAt DESC,
      p.id DESC
    """)
    Page<Post> findDefaultFeed(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT p FROM Post p
        WHERE p.id = :id AND p.active = true
        """)
    Optional<Post> findActiveById(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :id")
    void incrementViews(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.referralCount = p.referralCount + 1 WHERE p.id = :id")
    void incrementReferralCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Post p SET p.active = false WHERE p.id = :id")
    void softDelete(@Param("id") UUID id);

    @Modifying
    @Query("""
        UPDATE Post p
           SET p.active = false,
               p.status = com.Reffr_Backend.module.feed.entity.PostStatus.EXPIRED
         WHERE p.active = true
           AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
           AND p.expiresAt < CURRENT_TIMESTAMP
    """)
    int expireOpenPosts();

    long countByAuthorIdAndActive(UUID authorId, boolean active);

    @EntityGraph(attributePaths = {"tags"})
    @Query("SELECT p FROM Post p WHERE p.id IN :ids")
    List<Post> findWithTagsByIds(@Param("ids") List<UUID> ids);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findFeedFirst(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
      AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findFeedNext(@Param("cursorCreatedAt") Instant cursorCreatedAt,
                            @Param("cursorId") UUID cursorId,
                            Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
      AND p.type = :type
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findFeedByTypeFirst(@Param("type") Post.PostType type, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.active = true
      AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
      AND p.type = :type
      AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
      AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findFeedByTypeNext(@Param("type") Post.PostType type,
                                  @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                  @Param("cursorId") UUID cursorId,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.author.id = :authorId
      AND p.active = true
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findByAuthorFirst(@Param("authorId") UUID authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
    SELECT p FROM Post p
    WHERE p.author.id = :authorId
      AND p.active = true
      AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId))
    ORDER BY p.createdAt DESC, p.id DESC
    """)
    Page<Post> findByAuthorNext(@Param("authorId") UUID authorId,
                                @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                @Param("cursorId") UUID cursorId,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
        SELECT p FROM Post p
        WHERE p.active = true
          AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
          AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
          AND (:type    IS NULL OR p.type = :type)
          AND (:company IS NULL OR LOWER(p.company) LIKE LOWER(CONCAT('%', :company, '%')))
          AND (:role    IS NULL OR LOWER(p.currentRole) LIKE LOWER(CONCAT('%', :role, '%')))
          AND (:minExp  IS NULL OR (p.minExperience IS NULL OR p.minExperience >= :minExp))
          AND (:maxExp  IS NULL OR (p.maxExperience IS NULL OR p.maxExperience <= :maxExp))
        ORDER BY (p.viewsCount + p.referralCount * 3) DESC, p.createdAt DESC, p.id DESC
        """)
    Page<Post> searchByTypeTrending(@Param("type") Post.PostType type,
                                    @Param("company") String company,
                                    @Param("role") String role,
                                    @Param("minExp") Integer minExp,
                                    @Param("maxExp") Integer maxExp,
                                    Pageable pageable);

    @Query("""
        SELECT p.company, COUNT(p)
        FROM Post p
        WHERE p.active = true
          AND p.status = com.Reffr_Backend.module.feed.entity.PostStatus.OPEN
        GROUP BY p.company
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> findTopHiringCompanies(Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.author.id IN (SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId)
          AND p.active = true
        ORDER BY p.createdAt DESC
    """)
    Page<Post> findByFollowedUsers(@Param("userId") UUID userId, Pageable pageable);
}
