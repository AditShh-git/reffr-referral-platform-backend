package com.Reffr_Backend.module.feed.service.impl;

import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.feed.dto.PostDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostTag;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.feed.service.PostService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // ── Create ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"posts", "public_posts", "feed"}, allEntries = true)
    @Idempotent
    public PostDto.Response createPost(PostDto.CreateRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));


        if (request.getMaxExperience() != null && request.getMinExperience() != null 
                && request.getMaxExperience() < request.getMinExperience()) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Maximum experience cannot be less than minimum experience");
        }

        Post post = Post.builder()
                .author(user)
                .type(request.getType())
                .content(SanitizerUtils.sanitize(request.getContent()))
                .company(SanitizerUtils.sanitize(request.getCompany()))
                .currentRole(SanitizerUtils.sanitize(request.getCurrentRole()))
                .location(SanitizerUtils.sanitize(request.getLocation()))
                .minExperience(request.getMinExperience())
                .maxExperience(request.getMaxExperience())
                .build();

        // Attach tags
        if (request.getTags() != null) {
            request.getTags().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .map(t -> PostTag.builder().post(post).tag(t.trim().toLowerCase()).build())
                    .forEach(post.getTags()::add);
        }

        Post saved = postRepository.save(post);
        log.info("Post created — id={} user={}",
                saved.getId(),
                user.getGithubUsername() != null ? user.getGithubUsername() : "unknown");
        return PostDto.Response.from(saved, userId);
    }

    // ── Read ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PostDto.Response getPost(UUID postId, UUID currentUserId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        // Increment view count async-style (fire-and-forget within transaction)
        postRepository.incrementViews(postId);

        return PostDto.Response.from(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getFeed(Instant lastCreatedAt, Pageable pageable, UUID userId) {
        Page<Post> posts = postRepository.findActiveFeed(lastCreatedAt, pageable);
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getFeedByType(Post.PostType type, Instant lastCreatedAt, Pageable pageable, UUID userId) {
        Page<Post> posts = postRepository.findActiveByType(type, lastCreatedAt, pageable);
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "feed", key = "#userId + '-' + #pageable.pageNumber")
    public org.springframework.data.domain.Page<PostDto.FeedResponse> getPersonalizedFeed(Pageable pageable, UUID userId) {
        User user = userRepository.findByIdWithProfile(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));
        
        java.util.Set<String> userSkills = user.getSkills().stream()
                .map(s -> s.getSkill().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        Page<Post> posts;
        if (userSkills.isEmpty()) {
            posts = postRepository.findDefaultFeed(pageable);
        } else {
            posts = postRepository.findPersonalizedFeed(java.util.List.copyOf(userSkills), pageable);
        }
        
        List<PostDto.FeedResponse> dtoList = posts.stream()
                .map(p -> {
                    List<String> matched = userSkills.isEmpty() ? List.of() : p.getTags().stream()
                            .map(PostTag::getTag)
                            .filter(t -> userSkills.contains(t.toLowerCase()))
                            .toList();
                    return PostDto.FeedResponse.from(p, matched);
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(dtoList, pageable, posts.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getMyPosts(Instant lastCreatedAt, Pageable pageable, UUID userId) {
        Page<Post> posts = postRepository.findByAuthorId(userId, lastCreatedAt, pageable);
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getUserPosts(UUID authorId, Instant lastCreatedAt, Pageable pageable, UUID currentUserId) {
        // Verify author exists
        if (!userRepository.existsById(authorId)) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found");
        }
        Page<Post> posts = postRepository.findByAuthorId(authorId, lastCreatedAt, pageable);
        return buildCursorResponse(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> searchPosts(
            String  company,
            String  role,
            Integer minExp,
            Integer maxExp,
            Instant lastCreatedAt,
            Pageable pageable,
            UUID     userId) {

        Page<Post> posts = postRepository.search(
                        emptyToNull(company),
                        emptyToNull(role),
                        minExp,
                        maxExp,
                        lastCreatedAt,
                        pageable);
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "public_posts", key = "#type.name() + '_' + #sort + '_' + #company + '_' + #role + '_' + #minExp + '_' + #maxExp", condition = "#lastCreatedAt == null")
    public CursorPagedResponse<PostDto.Response> getPosts(
            Post.PostType type,
            String        sort,
            String        company,
            String        role,
            Integer       minExp,
            Integer       maxExp,
            Instant       lastCreatedAt,
            Pageable      pageable,
            UUID          userId) {

        String c = emptyToNull(company);
        String r = emptyToNull(role);

        Page<Post> posts;
        if ("trending".equalsIgnoreCase(sort)) {
            // Trending sort ignores 'lastCreatedAt' easily unless logic is explicitly added 
            posts = postRepository.searchByTypeTrending(type, c, r, minExp, maxExp, pageable);
        } else {
            posts = postRepository.searchByTypeLatest(type, c, r, minExp, maxExp, lastCreatedAt, pageable);
        }
        return buildCursorResponse(posts, userId);
    }

    // ── Update ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"posts", "public_posts", "feed"}, allEntries = true)
    public PostDto.Response updatePost(UUID postId, PostDto.UpdateRequest request, UUID userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCodes.POST_EDIT_DENIED, "You can only edit your own posts");
        }

        if (StringUtils.hasText(request.getContent()))  post.setContent(request.getContent().trim());
        if (StringUtils.hasText(request.getCompany()))  post.setCompany(request.getCompany().trim());
        if (StringUtils.hasText(request.getCurrentRole())) post.setCurrentRole(request.getCurrentRole().trim());
        if (StringUtils.hasText(request.getLocation())) post.setLocation(request.getLocation().trim());
        
        if (request.getMinExperience() != null)         post.setMinExperience(request.getMinExperience());
        if (request.getMaxExperience() != null)         post.setMaxExperience(request.getMaxExperience());

        if (post.getMaxExperience() != null && post.getMinExperience() != null 
                && post.getMaxExperience() < post.getMinExperience()) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Maximum experience cannot be less than minimum experience");
        }

        // Replace tags if provided
        if (request.getTags() != null) {
            post.getTags().clear();
            request.getTags().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .map(t -> PostTag.builder().post(post).tag(t.trim().toLowerCase()).build())
                    .forEach(post.getTags()::add);
        }

        Post saved = postRepository.save(post);
        log.info("Post updated — id={} user={}", postId, userId);
        return PostDto.Response.from(saved, userId);
    }

    // ── Delete ────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"posts", "public_posts", "feed"}, allEntries = true)
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCodes.POST_DELETE_DENIED, "You can only delete your own posts");
        }

        postRepository.softDelete(postId);
        log.info("Post deleted — id={} user={}", postId, userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }

    private CursorPagedResponse<PostDto.Response> buildCursorResponse(Page<Post> posts, UUID userId) {
        List<PostDto.Response> dtoList = posts.stream()
                .map(p -> PostDto.Response.from(p, userId))
                .toList();

        Instant nextCursor = null;
        if (!posts.isEmpty() && posts.hasNext()) {
            nextCursor = posts.getContent().get(posts.getContent().size() - 1).getCreatedAt();
        }

        return new CursorPagedResponse<>(dtoList, nextCursor, posts.hasNext());
    }

}