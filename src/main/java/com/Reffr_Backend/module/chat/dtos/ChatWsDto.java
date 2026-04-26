package com.Reffr_Backend.module.chat.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public final class ChatWsDto {

    private ChatWsDto() {}

    // ── Send message via WebSocket ────────────────────────────────────

    @Getter
    @Setter
    public static class SendMessageRequest {
        @NotBlank
        @Size(max = 2000)
        private String content;
    }

    // ── Typing indicator ─────────────────────────────────────────────
    
    @Getter @Setter
    public static class TypingRequest {
        private boolean typing;
    }

    @Getter @Builder
    public static class TypingEvent {
        private UUID userId;
        private boolean typing;
    }

    // ── Status update event (READ / DELIVERED) ───────────────────────

    @Getter @Builder
    public static class MessageStatusUpdateEvent {
        private UUID chatId;
        private java.util.List<UUID> messageIds;
        private com.Reffr_Backend.module.chat.entity.MessageStatus status;
        private java.time.Instant timestamp;
    }
}
