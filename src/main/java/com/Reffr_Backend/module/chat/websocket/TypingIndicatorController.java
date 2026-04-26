package com.Reffr_Backend.module.chat.websocket;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.chat.entity.Chat;
import com.Reffr_Backend.module.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * Typing indicator — client sends when user starts/stops typing.
 * Broadcasts to the chat topic so the other side shows "typing..."
 * No persistence — ephemeral event only.
 */
@Controller
@RequiredArgsConstructor
public class TypingIndicatorController {

    private final ChatRepository chatRepository;
    private final WebSocketMessagePublisher publisher;

    @MessageMapping("/chat/{chatId}/typing")
    public void typing(
            @DestinationVariable UUID chatId,
            com.Reffr_Backend.module.chat.dtos.ChatWsDto.TypingRequest request,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        // Verify user is a participant before broadcasting
        Chat chat = chatRepository.findActiveById(chatId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CHAT_NOT_FOUND, "Chat not found"));

        if (!chat.isParticipant(userId)) return;

        publisher.publishTyping(chatId, userId, request.isTyping());
    }
}