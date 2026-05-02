package com.Reffr_Backend.module.feed.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight Q&A attached to a Post.
 * Replaces the need to open a chat just to ask "is this role remote?" or
 * "are you still accepting applicants?".
 *
 * <p>Author of the post answers questions asynchronously.
 * No chat is created. No referral is required.
 */
@Entity
@Table(
        name = "post_questions",
        indexes = {
                @Index(name = "idx_pq_post",       columnList = "post_id, created_at ASC"),
                @Index(name = "idx_pq_asker",      columnList = "asker_id"),
                @Index(name = "idx_pq_unanswered", columnList = "post_id")
        }
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asker_id", nullable = false)
    private User asker;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at")
    private Instant answeredAt;

    /** Soft-hide support: author can hide inappropriate questions. */
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private boolean visible = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Domain helpers ────────────────────────────────────────────────────

    public boolean isAnswered() {
        return this.answer != null && !this.answer.isBlank();
    }

    public void answerWith(String answerText) {
        this.answer     = answerText;
        this.answeredAt = Instant.now();
    }
}
