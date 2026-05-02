package com.Reffr_Backend.module.feed.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import com.Reffr_Backend.common.util.PaginationUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Feed — create, browse and manage posts")
@RequiresOnboarding
public class PostController {

    private final PostService postService;

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a post",
            description = "SEEKER posts REQUEST (looking for referral). REFERRER posts OFFER (can refer).")
    public ResponseEntity<ApiResponse<PostDto.Response>> createPost(
            @Valid @RequestBody PostDto.CreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PostDto.Response post = postService.createPost(request, userId);
        return ResponseEntity.status(201).body(ApiResponse.success("Post created", post));
    }

    @GetMapping("/personalized")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Personalized feed",
            description = "Skill-based post recommendations for the authenticated user.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.FeedResponse>>> getPersonalizedFeed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                postService.getPersonalizedFeed(pageable, SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/following")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Following feed",
            description = "Posts only from users the current user follows.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getFollowingFeed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.Response> result = postService.getFollowingFeed(pageable, userId);
        if (result.getItems().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Follow users to personalize your feed", result));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "Browse and search posts",
            description = "Consolidated API for feed, search, and filtering. Use sort=latest for cursor-based pagination, or sort=trending for offset-based pagination.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getPosts(
            @RequestParam(required = false) Post.PostType type,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer minExp,
            @RequestParam(required = false) Integer maxExp,
            @RequestParam(required = false) Instant cursor,
            @RequestParam(required = false) UUID cursorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PostDto.SearchFilters filters = PostDto.SearchFilters.builder()
                .type(type)
                .sort(sort)
                .company(company)
                .role(role)
                .minExp(minExp)
                .maxExp(maxExp)
                .cursor(cursor)
                .cursorId(cursorId)
                .build();
        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.Response> result = postService.getPosts(
                filters, pageable, tryGetCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{postId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
    @Operation(summary = "Get a single post — increments view count")
    public ResponseEntity<ApiResponse<PostDto.Response>> getPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.getPost(postId, tryGetCurrentUserId())));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My posts — management view (edit / delete)")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getMyPosts(
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(required = false) UUID lastId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                postService.getMyPosts(lastCreatedAt, lastId, pageable, SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/user/{authorId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
    @Operation(summary = "Posts by user — public profile view")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getUserPosts(
            @PathVariable UUID authorId,
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(required = false) UUID lastId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                postService.getUserPosts(authorId, lastCreatedAt, lastId, pageable, tryGetCurrentUserId())));
    }

    @PutMapping("/{postId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit a post — owner only, partial update")
    public ResponseEntity<ApiResponse<PostDto.Response>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody PostDto.UpdateRequest request) {
        PostDto.Response updated = postService.updatePost(
                postId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post updated", updated));
    }

    @DeleteMapping("/{postId:[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a post — owner only, soft-delete")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }

    @PatchMapping("/{postId:[a-fA-F0-9-]{36}}/mark-fulfilled")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark a post as fulfilled (owner only)")
    public ResponseEntity<ApiResponse<Void>> markFulfilled(@PathVariable UUID postId) {
        postService.markFulfilled(postId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post marked as fulfilled ✓", null));
    }

    @PatchMapping("/{postId:[a-fA-F0-9-]{36}}/close")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Manually close a post (owner only)")
    public ResponseEntity<ApiResponse<Void>> closePost(@PathVariable UUID postId) {
        postService.closePost(postId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post closed", null));
    }

    private UUID tryGetCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
