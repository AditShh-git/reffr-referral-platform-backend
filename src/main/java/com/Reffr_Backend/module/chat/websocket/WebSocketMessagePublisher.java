package com.Reffr_Backend.module.chat.websocket;

import com.Reffr_Backend.module.chat.dtos.ChatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes chat events to WebSocket topics.
 * Injected into ChatServiceImpl so REST and WS share the same push logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishMessage(UUID chatId, ChatDto.MessageResponse message) {
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
            log.debug("WS publish — chatId={}", chatId);
        } catch (Exception e) {
            // WebSocket push failure must never break the REST response
            log.warn("WS publish failed — chatId={} error={}", chatId, e.getMessage());
        }
    }

    public void publishTyping(UUID chatId, UUID userId, boolean typing) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId + "/typing",
                    com.Reffr_Backend.module.chat.dtos.ChatWsDto.TypingEvent.builder()
                            .userId(userId)
                            .typing(typing)
                            .build()
            );
        } catch (Exception e) {
            log.warn("WS typing publish failed — chatId={}", chatId);
        }
    }

    public void publishStatusUpdate(UUID chatId, java.util.List<UUID> messageIds, com.Reffr_Backend.module.chat.entity.MessageStatus status) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId + "/status",
                    com.Reffr_Backend.module.chat.dtos.ChatWsDto.MessageStatusUpdateEvent.builder()
                            .chatId(chatId)
                            .messageIds(messageIds)
                            .status(status)
                            .timestamp(java.time.Instant.now())
                            .build()
            );
        } catch (Exception e) {
            log.warn("WS status update failed — chatId={}", chatId);
        }
    }
}
