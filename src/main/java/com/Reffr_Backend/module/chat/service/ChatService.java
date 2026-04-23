package com.Reffr_Backend.module.chat.service;

import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.chat.dtos.ChatDto;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface ChatService {

    /**
     * Called internally when a referral is accepted — creates or returns chat
     */
    ChatDto.ChatSummary openChat(UUID referralId, UUID currentUserId);

    /**
     * My inbox — paginated chats sorted by latest activity
     */
    CursorPagedResponse<ChatDto.ChatSummary> getMyChats(UUID userId, Instant lastMessageAt, Pageable pageable);

    /**
     * Load messages for a chat (paginated, latest first)
     */
    CursorPagedResponse<ChatDto.MessageResponse> getMessages(UUID chatId, UUID userId, Instant lastCreatedAt, Pageable pageable);

    /**
     * Send a message — only participants allowed
     */
    ChatDto.MessageResponse sendMessage(UUID chatId, String content, UUID senderId);

    /**
     * Mark all messages in a chat as read
     */
    void markAsRead(UUID chatId, UUID userId);

    /** Total unread count across all chats (notification badge) */
    long getUnreadCount(UUID userId);

    void markDelivered(UUID chatId, UUID userId);

    // ── Workflow actions ──────────────────────────────────────────

    /** Referrer marks the referral as submitted to the company. */
    ChatDto.ChatSummary markReferred(UUID chatId, UUID userId);
}