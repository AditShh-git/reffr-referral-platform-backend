package com.Reffr_Backend.module.feed.dto;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostTag;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;


import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PostDto {

    private PostDto() {}

    // ── Create ────────────────────────────────────────────────────────

    @Getter @Setter
    public static class CreateRequest {

        @NotNull(message = "Post type is required (REQUEST or OFFER)")
        private Post.PostType type;

        @NotBlank(message = "Content is required")
        @Size(min = 10, max = 1000, message = "Content must be 10–1000 characters")
        private String content;

        @Size(max = 150)
        private String company;

        @Size(max = 150)
        private String currentRole;

        @Size(max = 150)
        private String location;

        @Min(value = 0, message = "Minimum experience cannot be negative")
        private Integer minExperience;

        @Min(value = 0, message = "Maximum experience cannot be negative")
        private Integer maxExperience;

        @Size(max = 10, message = "Maximum 10 tags per post")
        private List<@Size(max = 80) String> tags;
    }

    // ── Update ────────────────────────────────────────────────────────

    @Getter @Setter
    public static class UpdateRequest {

        @Size(min = 10, max = 1000)
        private String content;

        @Size(max = 150)
        private String company;

        @Size(max = 150)
        private String currentRole;

        @Size(max = 150)
        private String location;

        private Integer minExperience;
        private Integer maxExperience;

        @Size(max = 10)
        private List<@Size(max = 80) String> tags;
    }

    // ── Response ──────────────────────────────────────────────────────


    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response implements Serializable {

        private UUID                id;
        private Post.PostType       type;
        private String              content;
        private String              company;
        private String              currentRole;
        private String              location;
        private Integer             minExperience;
        private Integer             maxExperience;
        private List<String>        tags;
        private Integer             viewsCount;
        private Integer             referralCount;
        private UserDto.UserSummary author;
        private Instant             createdAt;
        private Instant             expiresAt;
        private boolean             isOwner;

        public static Response from(Post post, UUID currentUserId) {
            return Response.builder()
                    .id(post.getId())
                    .type(post.getType())
                    .content(post.getContent())
                    .company(post.getCompany())
                    .currentRole(post.getCurrentRole())
                    .location(post.getLocation())
                    .minExperience(post.getMinExperience())
                    .maxExperience(post.getMaxExperience())
                    .tags(post.getTags().stream().map(PostTag::getTag).toList())
                    .viewsCount(post.getViewsCount())
                    .referralCount(post.getReferralCount())
                    .author(UserDto.UserSummary.from(post.getAuthor()))
                    .createdAt(post.getCreatedAt())
                    .expiresAt(post.getExpiresAt())
                    .isOwner(currentUserId != null
                            && post.getAuthor().getId().equals(currentUserId))
                    .build();
        }

        public static Response from(Post post) {
            return from(post, null);
        }
    }

    // ── Feed Response ──────────────────────────────────────────────────

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeedResponse implements Serializable {
        private UUID          postId;
        private Post.PostType type;
        private String        title; // mapped from post role or cropped content
        private String        company;
        private String        authorName;
        private boolean       isVerifiedAuthor;
        private Instant       createdAt;
        private List<String>  matchedSkills;

        public static FeedResponse from(Post post, List<String> matchedSkills) {
            String titleStr = post.getCurrentRole() != null ? post.getCurrentRole() : 
                (post.getContent().length() > 50 ? post.getContent().substring(0, 50) + "..." : post.getContent());
                
            return FeedResponse.builder()
                    .postId(post.getId())
                    .type(post.getType())
                    .title(titleStr)
                    .company(post.getCompany())
                    .authorName(post.getAuthor().getName())
                    .isVerifiedAuthor(post.getAuthor().isVerified())
                    .createdAt(post.getCreatedAt())
                    .matchedSkills(matchedSkills)
                    .build();
        }
    }
}