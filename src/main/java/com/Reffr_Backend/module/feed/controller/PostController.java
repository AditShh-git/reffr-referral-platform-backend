package com.Reffr_Backend.module.feed.controller;

import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.Reffr_Backend.common.security.annotation.RequiresOnboarding;
import org.springframework.data.domain.Page;
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

    // ── Create ────────────────────────────────────────────────────────

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

    // ── Feed — two tabs ───────────────────────────────────────────────

    @GetMapping("/requests")
    @Operation(summary = "Seeker tab — REQUEST posts",
            description = "Job seekers looking for referrals. Sort: latest (default) or trending.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getRequests(
            @RequestParam(defaultValue = "latest") String  sort,
            @RequestParam(required = false)         String  company,
            @RequestParam(required = false)         String  role,
            @RequestParam(required = false)         Integer minExp,
            @RequestParam(required = false)         Integer maxExp,
            @RequestParam(required = false)         Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")       int     page,
            @RequestParam(defaultValue = "20")      int     size) {

        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.Response> result = postService.getPosts(
                Post.PostType.REQUEST, sort, company, role, minExp, maxExp, lastCreatedAt, pageable,
                tryGetCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/offers")
    @Operation(summary = "Referrer tab — OFFER posts",
            description = "Employees offering referrals. Sort: latest (default) or trending.")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getOffers(
            @RequestParam(defaultValue = "latest") String  sort,
            @RequestParam(required = false)         String  company,
            @RequestParam(required = false)         String  role,
            @RequestParam(required = false)         Integer minExp,
            @RequestParam(required = false)         Integer maxExp,
            @RequestParam(required = false)         Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")       int     page,
            @RequestParam(defaultValue = "20")      int     size) {

        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.Response> result = postService.getPosts(
                Post.PostType.OFFER, sort, company, role, minExp, maxExp, lastCreatedAt, pageable,
                tryGetCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Search ────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search all posts — company, role, experience range")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> searchPosts(
            @RequestParam(required = false) String  company,
            @RequestParam(required = false) String  role,
            @RequestParam(required = false) Integer minExp,
            @RequestParam(required = false) Integer maxExp,
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PaginationUtils.of(page, size);
        CursorPagedResponse<PostDto.Response> result = postService.searchPosts(
                company, role, minExp, maxExp, lastCreatedAt, pageable, tryGetCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Single post ───────────────────────────────────────────────────

    @GetMapping("/{postId}")
    @Operation(summary = "Get a single post — increments view count")
    public ResponseEntity<ApiResponse<PostDto.Response>> getPost(@PathVariable UUID postId) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.getPost(postId, tryGetCurrentUserId())));
    }

    // ── My posts ──────────────────────────────────────────────────────

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "My posts — management view (edit / delete)")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getMyPosts(
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                postService.getMyPosts(lastCreatedAt, pageable, SecurityUtils.getCurrentUserId())));
    }

    // ── User posts (public) ───────────────────────────────────────────

    @GetMapping("/user/{authorId}")
    @Operation(summary = "Posts by user — public profile view")
    public ResponseEntity<ApiResponse<CursorPagedResponse<PostDto.Response>>> getUserPosts(
            @PathVariable UUID authorId,
            @RequestParam(required = false) Instant lastCreatedAt,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PaginationUtils.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(
                postService.getUserPosts(authorId, lastCreatedAt, pageable, tryGetCurrentUserId())));
    }

    // ── Update ────────────────────────────────────────────────────────

    @PutMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edit a post — owner only, partial update")
    public ResponseEntity<ApiResponse<PostDto.Response>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody PostDto.UpdateRequest request) {

        PostDto.Response updated = postService.updatePost(
                postId, request, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post updated", updated));
    }

    // ── Delete ────────────────────────────────────────────────────────

    @DeleteMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a post — owner only, soft-delete")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }

    // ── Helper ────────────────────────────────────────────────────────

    /**
     * Returns current userId if logged in, null if unauthenticated.
     * Feeds are public — auth is optional, but isOwner flag needs userId when available.
     */
    private UUID tryGetCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}