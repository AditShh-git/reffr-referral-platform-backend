package com.Reffr_Backend.module.notification.dto;

import com.Reffr_Backend.module.notification.entity.Notification;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

public final class NotificationDto {

    private NotificationDto() {}

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        private UUID             id;
        private NotificationType type;
        private String           title;
        private String           body;
        private String           entityType;  // "REFERRAL" | "CHAT" | "POST"
        private String           entityId;    // UUID of the related entity
        private boolean          isRead;
        private LocalDateTime    readAt;
        private LocalDateTime    createdAt;

        public static Response from(Notification n) {
            return Response.builder()
                    .id(n.getId())
                    .type(n.getType())
                    .title(n.getTitle())
                    .body(n.getBody())
                    .entityType(n.getEntityType())
                    .entityId(n.getEntityId())
                    .isRead(n.isRead())
                    .readAt(n.getReadAt())
                    .createdAt(n.getCreatedAt())
                    .build();
        }
    }

    /** Lightweight summary for the bell icon badge */
    @Getter
    @Builder
    public static class UnreadSummary {
        private long unreadCount;
    }
}
