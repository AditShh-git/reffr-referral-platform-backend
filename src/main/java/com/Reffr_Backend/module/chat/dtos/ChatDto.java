package com.Reffr_Backend.module.chat.dtos;

import com.Reffr_Backend.module.chat.entity.Chat;
import com.Reffr_Backend.module.chat.entity.ChatWorkflowStatus;
import com.Reffr_Backend.module.chat.entity.Message;
import com.Reffr_Backend.module.chat.entity.MessageType;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ChatDto {

    private ChatDto() {}

    // ── Send message request ──────────────────────────────────────────

    @Getter @Setter
    public static class SendMessageRequest {

        @NotBlank(message = "Message content is required")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        private String content;
    }

    // ── Chat summary (for inbox list) ─────────────────────────────────

    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatSummary {

        private UUID                  id;
        private UUID                  referralId;
        private UserDto.UserSummary   otherUser;
        private long                  unreadCount;
        private LocalDateTime         createdAt;
        private String                lastMessage;
        private LocalDateTime         lastMessageAt;
        private ChatWorkflowStatus    workflowStatus;

        public static ChatSummary from(Chat chat, UUID currentUserId, long unreadCount) {

            boolean isSeeker = chat.getSeeker().getId().equals(currentUserId);

            UserDto.UserSummary other = isSeeker
                    ? UserDto.UserSummary.from(chat.getReferrer())
                    : UserDto.UserSummary.from(chat.getSeeker());

            return ChatSummary.builder()
                    .id(chat.getId())
                    .referralId(chat.getReferral().getId())
                    .otherUser(other)
                    .unreadCount(unreadCount)
                    .createdAt(chat.getCreatedAt())
                    .lastMessage(chat.getLastMessage())
                    .lastMessageAt(chat.getLastMessageAt())
                    .workflowStatus(chat.getWorkflowStatus())
                    .build();
        }
    }

    // ── Message response ──────────────────────────────────────────────

    @Getter @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageResponse {

        private UUID                id;
        private UUID                chatId;
        private UserDto.UserSummary sender;    // null for SYSTEM messages
        private String              content;
        private MessageType         messageType;
        private boolean             isRead;
        private LocalDateTime       readAt;
        private LocalDateTime       createdAt;
        private boolean             isMine;
        private com.Reffr_Backend.module.chat.entity.MessageStatus status;

        public static MessageResponse from(Message message, UUID currentUserId) {
            boolean isSystem = message.getMessageType() == MessageType.SYSTEM;
            return MessageResponse.builder()
                    .id(message.getId())
                    .chatId(message.getChat().getId())
                    .sender(isSystem ? null : UserDto.UserSummary.from(message.getSender()))
                    .content(message.getContent())
                    .messageType(message.getMessageType())
                    .isRead(message.isRead())
                    .readAt(message.getReadAt())
                    .createdAt(message.getCreatedAt())
                    .isMine(!isSystem && message.getSender().getId().equals(currentUserId))
                    .status(message.getStatus())
                    .build();
        }
    }
}
