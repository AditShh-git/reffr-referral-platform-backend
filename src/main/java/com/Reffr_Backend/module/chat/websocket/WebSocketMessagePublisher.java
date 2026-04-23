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

    public void publishTyping(UUID chatId, UUID userId) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatId + "/typing",
                    TypingEvent.of(chatId, userId)
            );
        } catch (Exception e) {
            log.warn("WS typing publish failed — chatId={}", chatId);
        }
    }

    public record TypingEvent(UUID chatId, UUID userId) {
        public static TypingEvent of(UUID chatId, UUID userId) {
            return new TypingEvent(chatId, userId);
        }
    }
}
