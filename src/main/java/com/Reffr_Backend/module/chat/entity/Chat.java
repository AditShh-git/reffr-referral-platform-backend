package com.Reffr_Backend.module.chat.entity;

import com.Reffr_Backend.module.referral.entity.ReferralRequest;
import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** One accepted referral → one chat (UNIQUE enforced in DB). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_id", unique = true)
    private ReferralRequest referral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seeker_id")
    private User seeker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id")
    private User referrer;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * Tracks the last time any message was sent in this chat.
     * Used by the inactivity scheduler to detect dead chats.
     * Initialised to createdAt; updated on every message send.
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_status", length = 30, nullable = false)
    @Builder.Default
    private ChatWorkflowStatus workflowStatus = ChatWorkflowStatus.ACCEPTED;

    // ── Domain helpers ─────────────────────────────────────────────────────

    public boolean isParticipant(UUID userId) {
        return seeker.getId().equals(userId) || referrer.getId().equals(userId);
    }

    public void updateLastMessage(String content) {
        this.lastMessage    = content.length() > 100 ? content.substring(0, 97) + "..." : content;
        this.lastMessageAt  = LocalDateTime.now();
        this.lastActivityAt = this.lastMessageAt;   // keep them in sync
    }

    /**
     * Advances the workflow status and persists an auto-generated SYSTEM message.
     * Returns the system message so callers can broadcast it via WebSocket.
     */
    public Message advanceWorkflow(ChatWorkflowStatus newStatus, String systemText) {
        this.workflowStatus  = newStatus;
        this.lastActivityAt  = LocalDateTime.now();
        Message sysMsg = Message.builder()
                .chat(this)
                .sender(null)          // null sender = system-generated
                .content(systemText)
                .messageType(MessageType.SYSTEM)
                .status(MessageStatus.SENT)
                .build();
        this.messages.add(sysMsg);
        updateLastMessage(systemText);
        return sysMsg;
    }
}