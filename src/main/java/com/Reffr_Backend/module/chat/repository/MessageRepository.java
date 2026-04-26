package com.Reffr_Backend.module.chat.repository;

import com.Reffr_Backend.module.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Paginated messages for a chat — latest first (client reverses for display) */
//    @Query("""
//        SELECT m FROM Message m
//        JOIN FETCH m.sender
//        WHERE m.chat.id = :chatId
//          AND (:lastCreatedAt IS NULL OR m.createdAt < :lastCreatedAt)
//        ORDER BY m.createdAt DESC, m.id DESC
//        """)
//    Page<Message> findByChatId(@Param("chatId") UUID chatId, @Param("lastCreatedAt") LocalDateTime lastCreatedAt, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId
        ORDER BY m.createdAt DESC
        """)
    Page<Message> findFirstPage(@Param("chatId") UUID chatId, Pageable pageable);

    @Query("""
        SELECT m FROM Message m
        WHERE m.chat.id = :chatId AND m.createdAt < :cursor
        ORDER BY m.createdAt DESC
        """)
    Page<Message> findNextPage(@Param("chatId") UUID chatId, @Param("cursor") Instant cursor, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.read = true, m.status = 'READ' WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.read = false")
    int markAllRead(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Message m SET m.status = 'DELIVERED' WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.status = 'SENT'")
    int markAllDelivered(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("SELECT m.id FROM Message m WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.read = false")
    List<UUID> findUnreadMessageIds(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    @Query("SELECT m.id FROM Message m WHERE m.chat.id = :chatId AND m.sender.id != :userId AND m.status = 'SENT'")
    List<UUID> findUndeliveredMessageIds(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    /** Count unread messages in a specific chat for a user */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.chat.id = :chatId
          AND m.sender.id != :userId
          AND m.read = false
        """)
    long countUnread(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId);

    @Modifying
    @Query("""
    UPDATE Message m
    SET m.status = 'DELIVERED'
    WHERE m.chat.id = :chatId
      AND m.sender.id != :userId
      AND m.status = 'SENT'
""")
    int markDelivered(UUID chatId, UUID userId);


}
