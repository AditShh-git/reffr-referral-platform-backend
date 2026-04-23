package com.Reffr_Backend.module.notification.service.impl;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.notification.dto.NotificationDto;
import com.Reffr_Backend.module.notification.entity.Notification;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.repository.NotificationRepository;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import java.time.ZoneOffset;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Send (async — never blocks the caller) ────────────────────────

    @Override
    @Async("notificationExecutor")
    @Transactional
    public void send(UUID recipientId,
                     NotificationType type,
                     String title,
                     String body,
                     String entityType,
                     String entityId) {
        try {
            Notification notification = Notification.builder()
                    .user(userRepository.getReferenceById(recipientId))
                    .type(type)
                    .title(title != null ? title : "")
                    .body(body)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();

            notificationRepository.save(notification);

            log.debug("Notification sent — type={} recipient={}", type, recipientId);

        } catch (Exception e) {
            // Never break main flow
            log.error("Failed to send notification — type={} recipient={}", type, recipientId, e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<NotificationDto.Response> getAll(UUID userId, Instant lastCreatedAt, Pageable pageable) {
        LocalDateTime cursor = lastCreatedAt != null ? LocalDateTime.ofInstant(lastCreatedAt, ZoneOffset.UTC) : null;
        Page<Notification> notifications = notificationRepository.findByUserId(userId, cursor, pageable);
        
        return buildCursorResponse(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<NotificationDto.Response> getUnread(UUID userId, Instant lastCreatedAt, Pageable pageable) {
        LocalDateTime cursor = lastCreatedAt != null ? LocalDateTime.ofInstant(lastCreatedAt, ZoneOffset.UTC) : null;
        Page<Notification> notifications = notificationRepository.findUnreadByUserId(userId, cursor, pageable);
        
        return buildCursorResponse(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    // ── Mark read ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markOneRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markOneRead(
                notificationId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new NotFoundException(ErrorCodes.REFERRAL_NOT_FOUND, "Notification not found");
        }
    }

    @Override
    @Transactional
    public void markAllRead(UUID userId) {
        int updated = notificationRepository.markAllRead(userId, LocalDateTime.now());
        log.debug("Marked {} notifications read — userId={}", updated, userId);
    }
    
    private CursorPagedResponse<NotificationDto.Response> buildCursorResponse(Page<Notification> notifications) {
        List<NotificationDto.Response> dtoList = notifications.stream()
                .map(NotificationDto.Response::from)
                .toList();

        Instant nextCursor = null;
        if (!notifications.isEmpty() && notifications.hasNext()) {
            LocalDateTime next = notifications.getContent().get(notifications.getContent().size() - 1).getCreatedAt();
            if (next != null) {
                nextCursor = next.toInstant(ZoneOffset.UTC);
            }
        }
        return new CursorPagedResponse<>(dtoList, nextCursor, notifications.hasNext());
    }
}
