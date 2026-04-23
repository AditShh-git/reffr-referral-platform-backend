package com.Reffr_Backend.module.notification.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    // Which entity this notification is about (e.g. "REFERRAL", "CHAT", "POST")
    @Column(name = "entity_type", length = 50)
    private String entityType;

    // The UUID of that entity — stored as string for flexibility
    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Domain helpers ─────────────────────────────────────────────────

    public void markRead() {
        this.read   = true;
        this.readAt = LocalDateTime.now();
    }
}
