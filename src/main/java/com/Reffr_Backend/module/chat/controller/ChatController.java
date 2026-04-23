package com.Reffr_Backend.module.chat.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.chat.dtos.ChatDto;
import com.Reffr_Backend.module.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import com.Reffr_Backend.common.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;

import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat", description = "Messaging — unlocked after referral is accepted")
@RequiresOnboarding
public class ChatController {

    private final ChatService chatService;

    // ── Open / get chat for a referral ────────────────────────────────

    @PostMapping("/referral/{referralId}")
    @Operation(summary = "Open chat for an accepted referral",
            description = "Creates the chat if it doesn't exist yet. "
                    + "Returns 400 if referral is not ACCEPTED.")
    public ResponseEntity<ApiResponse<ChatDto.ChatSummary>> openChat(
            @PathVariable UUID referralId) {

        UUID userId = SecurityUtils.getCurrentUserId();
        ChatDto.ChatSummary chat = chatService.openChat(referralId, userId);
        return ResponseEntity.ok(ApiResponse.success(chat));
    }

    // ── My inbox ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "My chat inbox — all active chats with unread counts")
    public ResponseEntity<ApiResponse<CursorPagedResponse<ChatDto.ChatSummary>>> getMyChats(
            @RequestParam(required = false) Instant lastMessageAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(chatService.getMyChats(userId, lastMessageAt, pageable)));
    }

    // ── Messages in a chat ────────────────────────────────────────────

    @GetMapping("/{chatId}/messages")
    @Operation(summary = "Get messages for a chat",
            description = "Paginated, latest first. Pass page=0 for most recent messages. "
                    + "Also marks all messages as read automatically.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<ChatDto.MessageResponse>>> getMessages(
            @PathVariable UUID chatId,
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "30") int size) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PaginationUtils.of(page, size);

        CursorPagedResponse<ChatDto.MessageResponse> messages = chatService.getMessages(chatId, userId, lastCreatedAt, pageable);

        // Auto mark-as-read when opening the chat
        chatService.markAsRead(chatId, userId);

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    // ── Send message ──────────────────────────────────────────────────

    @PostMapping("/{chatId}/messages")
    @Operation(summary = "Send a message",
            description = "Only participants (seeker + referrer) of this chat can send messages.")
    public ResponseEntity<ApiResponse<ChatDto.MessageResponse>> sendMessage(
            @PathVariable UUID chatId,
            @Valid @RequestBody ChatDto.SendMessageRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        ChatDto.MessageResponse message = chatService.sendMessage(chatId, request.getContent(), userId);
        return ResponseEntity.status(201).body(ApiResponse.success("Message sent", message));
    }

    // ── Mark as read ──────────────────────────────────────────────────

    @PatchMapping("/{chatId}/read")
    @Operation(summary = "Mark all messages in a chat as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID chatId) {
        chatService.markAsRead(chatId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    // ── Unread badge count ────────────────────────────────────────────

    @GetMapping("/unread-count")
    @Operation(summary = "Total unread message count — for notification badge")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        long count = chatService.getUnreadCount(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ── Referral workflow actions ─────────────────────────────────────

    @PostMapping("/{chatId}/mark-referred")
    @Operation(summary = "Mark referral as submitted",
               description = "Referrer confirms they have submitted the candidate's profile to the company. Advances workflow from ACCEPTED → REFERRED.")
    public ResponseEntity<ApiResponse<ChatDto.ChatSummary>> markReferred(@PathVariable UUID chatId) {
        ChatDto.ChatSummary summary = chatService.markReferred(chatId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Referral marked as submitted", summary));
    }
}
