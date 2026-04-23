package com.Reffr_Backend.module.feed.entity;

import com.Reffr_Backend.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 150)
    private String company;

    @Column(name = "role", length = 150)
    private String currentRole;

    @Column(name = "min_experience")
    private Integer minExperience;

    @Column(name = "max_experience")
    private Integer maxExperience;

    @Column(length = 150)
    private String location;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "views_count")
    @Builder.Default
    private Integer viewsCount = 0;

    @Column(name = "referral_count")
    @Builder.Default
    private Integer referralCount = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostTag> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum PostType { REQUEST, OFFER }

    public boolean isOwnedBy(UUID userId) {
        return this.author != null &&
                this.author.getId().equals(userId);
    }

    public void incrementReferralCount() { this.referralCount++; }
    public void incrementViewsCount()    { this.viewsCount++;    }
}
