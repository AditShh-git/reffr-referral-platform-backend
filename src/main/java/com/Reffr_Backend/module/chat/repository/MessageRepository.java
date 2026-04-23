package com.Reffr_Backend.module.chat.repository;

import com.Reffr_Backend.module.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /** Paginated messages for a chat — latest first (client reverses for display) */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.sender
        WHERE m.chat.id = :chatId
          AND (:lastCreatedAt IS NULL OR m.createdAt < :lastCreatedAt)
        ORDER BY m.createdAt DESC, m.id DESC
        """)
    Page<Message> findByChatId(@Param("chatId") UUID chatId, @Param("lastCreatedAt") LocalDateTime lastCreatedAt, Pageable pageable);

    /** Mark all unread messages in a chat as read (called when user opens the chat) */
    @Modifying
    @Query("""
        UPDATE Message m
        SET m.read = true, m.readAt = :now
        WHERE m.chat.id = :chatId
          AND m.sender.id != :userId
          AND m.read = false
        """)
    int markAllRead(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("now")    LocalDateTime now);

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
