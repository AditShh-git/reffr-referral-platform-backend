package com.Reffr_Backend.module.notification.repository;

import com.Reffr_Backend.module.notification.entity.Notification;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // ── Feed ──────────────────────────────────────────────────────────

//    @Query("""
//        SELECT n FROM Notification n
//        WHERE n.user.id = :userId
//          AND (:lastCreatedAt IS NULL OR n.createdAt < :lastCreatedAt)
//        ORDER BY n.createdAt DESC, n.id DESC
//        """)
//    Page<Notification> findByUserId(@Param("userId") UUID userId, @Param("lastCreatedAt") LocalDateTime lastCreatedAt, Pageable pageable);

    // without cursor
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    // with cursor
    @Query("""
    SELECT n FROM Notification n
    WHERE n.user.id = :userId
      AND n.createdAt < :cursor
    ORDER BY n.createdAt DESC, n.id DESC
""")
    Page<Notification> findByUserIdWithCursor(UUID userId, LocalDateTime cursor, Pageable pageable);

//    @Query("""
//        SELECT n FROM Notification n
//        WHERE n.user.id = :userId
//          AND n.read = false
//          AND (:lastCreatedAt IS NULL OR n.createdAt < :lastCreatedAt)
//        ORDER BY n.createdAt DESC, n.id DESC
//        """)
//    Page<Notification> findUnreadByUserId(@Param("userId") UUID userId, @Param("lastCreatedAt") LocalDateTime lastCreatedAt, Pageable pageable);

    Page<Notification> findByUserIdAndReadFalse(UUID userId, Pageable pageable);

    @Query("""
    SELECT n FROM Notification n
    WHERE n.user.id = :userId
      AND n.read = false
      AND n.createdAt < :cursor
    ORDER BY n.createdAt DESC, n.id DESC
""")
    Page<Notification> findUnreadByUserIdWithCursor(
            UUID userId,
            LocalDateTime cursor,
            Pageable pageable
    );

    // ── Counts ────────────────────────────────────────────────────────

    long countByUserIdAndRead(UUID userId, boolean read);

    long countByUserIdAndType(UUID userId, NotificationType type);

    boolean existsByUserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtAfter(
            UUID userId,
            com.Reffr_Backend.module.notification.entity.NotificationType type,
            String entityType,
            String entityId,
            LocalDateTime createdAt);

    @Query("""
        SELECT n.entityId FROM Notification n
        WHERE n.user.id = :userId
          AND n.type = com.Reffr_Backend.module.notification.entity.NotificationType.PROFILE_VIEWED
        ORDER BY n.createdAt DESC
        """)
    List<String> findRecentProfileViewerEntityIds(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
          AND n.type = com.Reffr_Backend.module.notification.entity.NotificationType.PROFILE_VIEWED
        ORDER BY n.createdAt DESC, n.id DESC
        """)
    List<Notification> findProfileViewNotifications(@Param("userId") UUID userId, Pageable pageable);

    // ── Bulk mutations ────────────────────────────────────────────────

    /** Mark all notifications as read for a user */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true, n.readAt = :now
        WHERE n.user.id = :userId AND n.read = false
        """)
    int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /** Mark a single notification as read */
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true, n.readAt = :now
        WHERE n.id = :id AND n.user.id = :userId
        """)
    int markOneRead(
            @Param("id")     UUID id,
            @Param("userId") UUID userId,
            @Param("now")    LocalDateTime now);

    /** Delete all notifications older than X days (cleanup job) */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before AND n.read = true")
    int deleteOldRead(@Param("before") LocalDateTime before);
}
