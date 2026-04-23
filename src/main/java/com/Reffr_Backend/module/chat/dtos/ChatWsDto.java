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

    // ── Read receipt event ───────────────────────────────────────────

    @Getter @Builder
    public static class ReadReceiptEvent {
        private UUID chatId;
        private UUID readByUserId;
    }
}
