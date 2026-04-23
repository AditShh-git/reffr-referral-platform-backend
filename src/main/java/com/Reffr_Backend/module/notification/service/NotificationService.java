package com.Reffr_Backend.module.notification.service;

import com.Reffr_Backend.module.notification.dto.NotificationDto;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.time.Instant;

public interface NotificationService {

    // ── Create (called internally by other services) ──────────────────

    void send(UUID recipientId,
              NotificationType type,
              String title,
              String body,
              String entityType,
              String entityId);

    // ── Read ──────────────────────────────────────────────────────────

    CursorPagedResponse<NotificationDto.Response> getAll(UUID userId, Instant lastCreatedAt, Pageable pageable);

    CursorPagedResponse<NotificationDto.Response> getUnread(UUID userId, Instant lastCreatedAt, Pageable pageable);

    long getUnreadCount(UUID userId);

    // ── Mark read ─────────────────────────────────────────────────────

    void markOneRead(UUID notificationId, UUID userId);

    void markAllRead(UUID userId);
}
