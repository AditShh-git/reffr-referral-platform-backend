package com.Reffr_Backend.module.user.entity;

import jakarta.persistence.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_skills_user_id_skill_name", columnNames = {"user_id", "skill_name"}))
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
