package com.Reffr_Backend.module.feed.dto;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostStatus;
import com.Reffr_Backend.module.feed.entity.PostTag;
import com.Reffr_Backend.module.feed.entity.PostVisibility;
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

    // ── Search Filters ────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchFilters {
        private Post.PostType type;
        private String company;
        private String role;
        private Integer minExp;
        private Integer maxExp;
        private String sort; // "latest" or "trending"
        private Instant cursor;
        private UUID cursorId;
        private UUID authorId;
    }

    // ── Create ────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class CreateRequest {

        @NotNull(message = "Post type is required (REQUEST or OFFER)")
        private Post.PostType type;

        /**
         * Optional short title. If null, the response derives one from content.
         * Example: "Looking for Entry Level Java Backend Role at Amazon"
         */
        @Size(max = 200, message = "Title must be 200 characters or fewer")
        private String title;

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

        // ── REQUEST post extras ───────────────────────────────────────

        @Size(max = 500)
        private String githubLink;

        @Size(max = 500)
        private String linkedinLink;

        /**
         * Controls who can see the resume snapshot URL in the response.
         * Defaults to PUBLIC when not supplied.
         */
        private PostVisibility resumeVisibility;

        /** Optional urgency deadline shown on the post card. */
        private Instant urgencyDeadline;
    }

    // ── Update ────────────────────────────────────────────────────────

    @Getter
    @Setter
    public static class UpdateRequest {

        @Size(max = 200)
        private String title;

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

        @Size(max = 500)
        private String githubLink;

        @Size(max = 500)
        private String linkedinLink;

        private PostVisibility resumeVisibility;

        private Instant urgencyDeadline;
    }

    // ── Response ──────────────────────────────────────────────────────

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response implements Serializable {

        private UUID                id;
        private Post.PostType       type;

        /**
         * Derived title: post.title if set; otherwise currentRole if set;
         * otherwise first 80 chars of content. Never null in response.
         */
        private String              title;

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

        // ── Lifecycle ─────────────────────────────────────────────────
        /** Fine-grained post lifecycle status. */
        private PostStatus          status;
        private Instant             createdAt;
        private Instant             expiresAt;
        private Instant             urgencyDeadline;
        private boolean             isOwner;

        // ── REQUEST post assets (visibility-gated) ────────────────────
        /**
         * Pre-signed S3 URL for the resume snapshot.
         * Null when: post is OFFER, visibility is PRIVATE, or viewer is not
         * verified and visibility is VERIFIED_ONLY.
         */
        private String              resumeSnapshotUrl;
        private String              resumeUrl;
        private String              githubLink;
        private String              linkedinLink;

        /**
         * Always returned so the client knows why resumeSnapshotUrl may be null.
         * Null for OFFER posts.
         */
        private PostVisibility      resumeVisibility;

        // ── Factory ──────────────────────────────────────────────────

        /**
         * Primary factory. Enforces visibility rules for resume fields.
         *
         * @param post            the post entity (must have author loaded)
         * @param currentUserId   viewer's UUID; null = unauthenticated
         * @param viewerVerified  true if the viewer has a CURRENT company verification
         * @param resumePresignedUrl pre-generated presigned URL (or null if not available)
         */
        public static Response from(Post post,
                                    UUID currentUserId,
                                    boolean viewerVerified,
                                    String resumePresignedUrl) {

            boolean isOwner = currentUserId != null
                    && post.getAuthor().getId().equals(currentUserId);

            // Derive title — never null in response
            String title = deriveTitle(post);

            // Asset visibility gate (only for REQUEST posts)
            String resolvedResumeUrl  = null;
            String resolvedGithubLink = null;
            String resolvedLinkedinLink = null;
            PostVisibility visibility = null;

            if (post.getType() == Post.PostType.REQUEST) {
                visibility = post.getResumeVisibility() != null
                        ? post.getResumeVisibility()
                        : PostVisibility.PUBLIC;

                resolvedGithubLink   = post.getGithubLink();
                resolvedLinkedinLink = post.getLinkedinLink();

                resolvedResumeUrl = switch (visibility) {
                    case PUBLIC        -> resumePresignedUrl;
                    case VERIFIED_ONLY -> (isOwner || viewerVerified) ? resumePresignedUrl : null;
                    case PRIVATE       -> null;    // never exposed; shared in chat
                };
            }

            return Response.builder()
                    .id(post.getId())
                    .type(post.getType())
                    .title(title)
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
                    .status(post.getStatus())
                    .createdAt(post.getCreatedAt())
                    .expiresAt(post.getExpiresAt())
                    .urgencyDeadline(post.getUrgencyDeadline())
                    .isOwner(isOwner)
                    .resumeSnapshotUrl(resolvedResumeUrl)
                    .resumeUrl(resolvedResumeUrl)
                    .githubLink(resolvedGithubLink)
                    .linkedinLink(resolvedLinkedinLink)
                    .resumeVisibility(visibility)
                    .build();
        }

        /**
         * Convenience overload for unauthenticated/anonymous callers.
         * viewerVerified = false, no presigned URL generated.
         */
        public static Response from(Post post, UUID currentUserId) {
            return from(post, currentUserId, false, null);
        }

        /** No-auth overload (public feed, anonymous viewer). */
        public static Response from(Post post) {
            return from(post, null, false, null);
        }

        // ── Private helpers ───────────────────────────────────────────

        /**
         * Derives a non-null display title from the post's data.
         * Priority: explicit title → currentRole → first 80 chars of content.
         * Handles old posts that have no title set.
         */
        private static String deriveTitle(Post post) {
            if (post.getTitle() != null && !post.getTitle().isBlank()) {
                return post.getTitle();
            }
            if (post.getCurrentRole() != null && !post.getCurrentRole().isBlank()) {
                String suffix = post.getCompany() != null && !post.getCompany().isBlank()
                        ? " at " + post.getCompany()
                        : "";
                return post.getCurrentRole() + suffix;
            }
            String c = post.getContent();
            return c.length() > 80 ? c.substring(0, 77) + "..." : c;
        }
    }

    // ── Feed Response (personalised) ──────────────────────────────────

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeedResponse implements Serializable {
        private UUID          postId;
        private Post.PostType type;
        private String        title;  // derived — never null
        private String        company;
        private String        authorName;
        private boolean       isVerifiedAuthor;
        private PostStatus    status;
        private Instant       createdAt;
        private List<String>  matchedSkills;

        public static FeedResponse from(Post post, List<String> matchedSkills) {
            return FeedResponse.builder()
                    .postId(post.getId())
                    .type(post.getType())
                    .title(Response.deriveTitle(post))   // reuse same derivation logic
                    .company(post.getCompany())
                    .authorName(post.getAuthor().getName())
                    .isVerifiedAuthor(post.getAuthor().isVerified())
                    .status(post.getStatus())
                    .createdAt(post.getCreatedAt())
                    .matchedSkills(matchedSkills)
                    .build();
        }
    }
}
