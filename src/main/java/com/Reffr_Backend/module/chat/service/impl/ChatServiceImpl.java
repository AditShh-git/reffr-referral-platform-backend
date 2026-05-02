package com.Reffr_Backend.module.chat.service.impl;

import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.module.chat.entity.MessageStatus;
import com.Reffr_Backend.module.chat.websocket.WebSocketMessagePublisher;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.notification.service.EmailService;
import com.Reffr_Backend.module.referral.entity.ReferralStatus;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.chat.dtos.ChatDto;
import com.Reffr_Backend.module.chat.entity.Chat;
import com.Reffr_Backend.module.chat.entity.ChatWorkflowStatus;
import com.Reffr_Backend.module.chat.entity.Message;
import com.Reffr_Backend.module.chat.repository.ChatRepository;
import com.Reffr_Backend.module.chat.repository.MessageRepository;
import com.Reffr_Backend.module.chat.service.ChatService;
import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.referral.repository.ReferralRequestRepository;
import com.Reffr_Backend.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ReferralRequestRepository referralRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final WebSocketMessagePublisher wsPublisher;

    // ── OPEN CHAT ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatDto.ChatSummary openChat(UUID referralId, UUID currentUserId) {

        ReferralRequest referral = referralRepository.findByIdWithDetails(referralId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Referral not found"));

        if (referral.getStatus() != ReferralStatus.ACCEPTED) {
            throw new BusinessException(ErrorCodes.CHAT_LOCKED,
                    "Chat available only after referral acceptance");
        }

        UUID seekerId   = referral.getRequester().getId();
        UUID referrerId = referral.getReferrer().getId();

        if (!currentUserId.equals(seekerId) && !currentUserId.equals(referrerId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Not part of this referral");
        }

        return chatRepository.findByReferralId(referralId)
                .map(existing -> buildSummary(existing, currentUserId))
                .orElseGet(() -> createChat(referral, currentUserId));
    }

    private ChatDto.ChatSummary createChat(ReferralRequest referral, UUID currentUserId) {
        Chat chat = Chat.builder()
                .referral(referral)
                .seeker(referral.getRequester())
                .referrer(referral.getReferrer())
                .build();

        try {
            Chat saved = chatRepository.save(chat);
            log.info("Chat created — chatId={} referralId={}", saved.getId(), referral.getId());
            return buildSummary(saved, currentUserId);

        } catch (DataIntegrityViolationException e) {
            // Race condition safe
            log.warn("Chat already exists — resolving race condition");
            Chat existing = chatRepository.findByReferralId(referral.getId())
                    .orElseThrow();
            return buildSummary(existing, currentUserId);
        }
    }

    // ── INBOX ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<ChatDto.ChatSummary> getMyChats(UUID userId, Instant lastMessageAt, Pageable pageable) {
        LocalDateTime cursor = lastMessageAt != null ? LocalDateTime.ofInstant(lastMessageAt, ZoneOffset.UTC) : null;
        Page<Chat> chats;

        if (cursor == null) {
            chats = chatRepository.findFirstPage(userId, pageable);
        } else {
            chats = chatRepository.findNextPage(userId, cursor, pageable);
        }

        List<ChatDto.ChatSummary> dtoList = chats.stream()
                .map(chat -> buildSummary(chat, userId))
                .toList();

        Instant nextCursor = null;
        if (!chats.isEmpty() && chats.hasNext()) {
            LocalDateTime next = chats.getContent().get(chats.getContent().size() - 1).getLastMessageAt();
            if (next != null) {
                nextCursor = next.toInstant(ZoneOffset.UTC);
            }
        }
        return new CursorPagedResponse<>(dtoList, nextCursor, null, chats.hasNext());
    }

    // ── MESSAGES ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<ChatDto.MessageResponse> getMessages(UUID chatId, UUID userId, Instant lastCreatedAt, Pageable pageable) {

        Chat chat = getChatForParticipant(chatId, userId);

        Page<Message> messages;

        if (lastCreatedAt == null) {
            messages = messageRepository.findFirstPage(chat.getId(), pageable);
        } else {
            messages = messageRepository.findNextPage(chat.getId(), lastCreatedAt, pageable);
        }
        
        List<ChatDto.MessageResponse> dtoList = messages.stream()
                .map(m -> ChatDto.MessageResponse.from(m, userId))
                .toList();

        Instant nextCursor = null;
        if (!messages.isEmpty() && messages.hasNext()) {
            LocalDateTime next = messages.getContent().get(messages.getContent().size() - 1).getCreatedAt();
            if (next != null) {
                nextCursor = next.toInstant(ZoneOffset.UTC);
            }
        }
        return new CursorPagedResponse<>(dtoList, nextCursor, null, messages.hasNext());
    }

    // ── SEND MESSAGE ──────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatDto.MessageResponse sendMessage(UUID chatId, String content, UUID senderId) {

        String cleanContent = content != null ? com.Reffr_Backend.common.util.SanitizerUtils.sanitize(content) : null;

        if (!StringUtils.hasText(cleanContent)) {
            throw new BusinessException(ErrorCodes.INVALID_MESSAGE, "Message cannot be empty");
        }

        Chat chat = getChatForParticipant(chatId, senderId);

        boolean isSeeker = chat.getSeeker().getId().equals(senderId);

        // No extra DB call
        User sender = isSeeker ? chat.getSeeker() : chat.getReferrer();
        UUID recipientId = isSeeker
                ? chat.getReferrer().getId()
                : chat.getSeeker().getId();

        Message message = Message.builder()
                .chat(chat)
                .sender(sender)
                .content(cleanContent)
                .status(MessageStatus.SENT)
                .build();

        Message saved = messageRepository.save(message);

        // Update chat preview (dirty checking)
        chat.updateLastMessage(cleanContent);

        ChatDto.MessageResponse response = ChatDto.MessageResponse.from(saved, senderId);

        // ── WebSocket push (real-time)
        wsPublisher.publishMessage(chatId, response);

        // ── Notification (async)
        notificationService.send(
                recipientId,
                NotificationType.NEW_MESSAGE,
                NotificationMessages.newMessageTitle(sender.getName()),
                NotificationMessages.newMessageBody(cleanContent),
                "CHAT",
                chatId.toString()
        );

        User recipient = isSeeker ? chat.getReferrer() : chat.getSeeker();
        if (recipient.getPrimaryEmail() != null) {
            emailService.sendNewMessage(recipient, sender.getName(), cleanContent, chatId);
        }

        log.info("Message sent — chatId={} sender={}", chatId, senderId);

        return response;
    }

    // ── MARK READ ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markAsRead(UUID chatId, UUID userId) {
        List<UUID> messageIds = messageRepository.findUnreadMessageIds(chatId, userId);
        if (messageIds.isEmpty()) return;

        messageRepository.markAllRead(chatId, userId);
        wsPublisher.publishStatusUpdate(chatId, messageIds, com.Reffr_Backend.module.chat.entity.MessageStatus.READ);
        log.debug("Chat marked read — chatId={} userId={}", chatId, userId);
    }

    // ── UNREAD COUNT ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return chatRepository.countUnreadMessages(userId);
    }

    @Override
    @Transactional
    public void markDelivered(UUID chatId, UUID userId) {
        List<UUID> messageIds = messageRepository.findUndeliveredMessageIds(chatId, userId);
        if (messageIds.isEmpty()) return;

        messageRepository.markAllDelivered(chatId, userId);
        wsPublisher.publishStatusUpdate(chatId, messageIds, com.Reffr_Backend.module.chat.entity.MessageStatus.DELIVERED);
        log.debug("Chat marked delivered — chatId={} userId={}", chatId, userId);
    }

    // ── WORKFLOW ACTIONS ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatDto.ChatSummary markReferred(UUID chatId, UUID userId) {
        Chat chat = getChatForParticipant(chatId, userId);
        requireWorkflowState(chat, ChatWorkflowStatus.ACCEPTED,
                "Referral can only be submitted from ACCEPTED state");

        // Only the referrer may trigger this step
        if (!chat.getReferrer().getId().equals(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Only the referrer can mark this as submitted");
        }

        Message sysMsg = chat.advanceWorkflow(
                ChatWorkflowStatus.REFERRED, "\uD83D\uDCE8 Referral has been submitted to the company");
        messageRepository.save(sysMsg);

        log.info("Workflow: REFERRED — chatId={} referrer={}", chatId, userId);

        emailService.sendReferralAccepted(chat.getSeeker(),
                chat.getReferrer().getName(), chatId);

        wsPublisher.publishMessage(chatId, ChatDto.MessageResponse.from(sysMsg, userId));

        return buildSummary(chat, userId);
    }


    // ── HELPERS ───────────────────────────────────────────────────────

    private Chat getChatForParticipant(UUID chatId, UUID userId) {

        Chat chat = chatRepository.findActiveById(chatId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.CHAT_NOT_FOUND, "Chat not found"));

        if (!chat.isParticipant(userId)) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Not a participant");
        }

        return chat;
    }

    private void requireWorkflowState(Chat chat, ChatWorkflowStatus required, String error) {
        if (chat.getWorkflowStatus() != required) {
            throw new BusinessException(ErrorCodes.INVALID_WORKFLOW_STATE,
                    error + " (current: " + chat.getWorkflowStatus() + ")");
        }
    }

    private ChatDto.ChatSummary buildSummary(Chat chat, UUID userId) {
        long unread = messageRepository.countUnread(chat.getId(), userId);
        return ChatDto.ChatSummary.from(chat, userId, unread);
    }
}
