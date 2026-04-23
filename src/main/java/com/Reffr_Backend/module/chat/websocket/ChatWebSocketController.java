package com.Reffr_Backend.module.chat.websocket;

import com.Reffr_Backend.module.chat.dtos.ChatDto;
import com.Reffr_Backend.module.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(
            @DestinationVariable UUID chatId,
            @Payload @Valid ChatDto.SendMessageRequest request,
            Principal principal) {

        UUID senderId = UUID.fromString(principal.getName());

        chatService.sendMessage(chatId, request.getContent(), senderId);

        log.debug("WS send triggered — chatId={} sender={}", chatId, senderId);
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markRead(
            @DestinationVariable UUID chatId,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        chatService.markAsRead(chatId, userId);
    }

    //  NEW — DELIVERED
    @MessageMapping("/chat/{chatId}/delivered")
    public void markDelivered(
            @DestinationVariable UUID chatId,
            Principal principal) {

        UUID userId = UUID.fromString(principal.getName());

        chatService.markDelivered(chatId, userId);
    }
}
