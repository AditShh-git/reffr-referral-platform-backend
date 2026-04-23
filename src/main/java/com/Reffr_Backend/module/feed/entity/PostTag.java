package com.Reffr_Backend.module.feed.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag"}))
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 80)
    private String tag;
}
