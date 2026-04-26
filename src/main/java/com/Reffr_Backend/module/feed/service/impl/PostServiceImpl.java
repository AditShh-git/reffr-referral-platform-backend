package com.Reffr_Backend.module.feed.service.impl;

import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.entity.PostTag;
import com.Reffr_Backend.module.feed.repository.PostRepository;
import com.Reffr_Backend.module.feed.repository.PostSpecification;
import com.Reffr_Backend.module.feed.service.PostEligibilityService;
import com.Reffr_Backend.module.feed.service.PostService;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserFollowRepository;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostEligibilityService postEligibilityService;
    private final UserFollowRepository userFollowRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {"posts", "public_posts", "feed"}, allEntries = true)
    @Idempotent
    public PostDto.Response createPost(PostDto.CreateRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        postEligibilityService.validateCreatePost(user, request.getType());

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

        if (request.getTags() != null) {
            request.getTags().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .map(t -> PostTag.builder().post(post).tag(t.trim().toLowerCase()).build())
                    .forEach(post.getTags()::add);
        }

        Post saved = postRepository.save(post);
        notifyFollowersAboutNewPost(saved);

        log.info("Post created - id={} user={}",
                saved.getId(),
                user.getGithubUsername() != null ? user.getGithubUsername() : "unknown");

        return PostDto.Response.from(saved, userId);
    }

    @Override
    @Transactional
    public PostDto.Response getPost(UUID postId, UUID currentUserId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        postRepository.incrementViews(postId);
        return PostDto.Response.from(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getPosts(PostDto.SearchFilters filters, Pageable pageable, UUID currentUserId) {
        // Hybrid logic: Latest -> Cursor, Trending -> Offset (Spec)
        if ("trending".equalsIgnoreCase(filters.getSort())) {
            return getTrendingPosts(filters, pageable, currentUserId);
        }
        
        // Latest sort logic
        if (isSimpleFilter(filters)) {
            return getLatestPostsSimple(filters, pageable, currentUserId);
        } else {
            return getLatestPostsWithSpec(filters, pageable, currentUserId);
        }
    }

    private boolean isSimpleFilter(PostDto.SearchFilters filters) {
        return !StringUtils.hasText(filters.getCompany()) &&
               !StringUtils.hasText(filters.getRole()) &&
               filters.getMinExp() == null &&
               filters.getMaxExp() == null &&
               filters.getAuthorId() == null;
    }

    private CursorPagedResponse<PostDto.Response> getLatestPostsSimple(PostDto.SearchFilters filters, Pageable pageable, UUID userId) {
        Page<Post> posts;
        Instant cursor = filters.getCursor();
        Post.PostType type = filters.getType();

        if (type == null) {
            posts = (cursor == null) ? postRepository.findFeedFirst(pageable) : postRepository.findFeedNext(cursor, pageable);
        } else {
            posts = (cursor == null) ? postRepository.findFeedByTypeFirst(type, pageable) : postRepository.findFeedByTypeNext(type, cursor, pageable);
        }
        return buildCursorResponse(posts, userId);
    }

    private CursorPagedResponse<PostDto.Response> getLatestPostsWithSpec(PostDto.SearchFilters filters, Pageable pageable, UUID userId) {
        // Even with latest sort, if complex filters are present, use Spec
        // Note: Specification search is offset-based as per design choice
        org.springframework.data.jpa.domain.Specification<Post> spec = PostSpecification.filterBy(
                filters.getType(), filters.getCompany(), filters.getRole(), filters.getMinExp(), filters.getMaxExp(), filters.getAuthorId()
        );
        
        // Ensure latest sort if not already in pageable
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                org.springframework.data.domain.Sort.by("createdAt").descending().and(org.springframework.data.domain.Sort.by("id").descending())
            );
        }

        Page<Post> posts = postRepository.findAll(spec, sortedPageable);
        return buildCursorResponse(posts, userId); // buildCursorResponse works for both cursor and offset pages
    }

    private CursorPagedResponse<PostDto.Response> getTrendingPosts(PostDto.SearchFilters filters, Pageable pageable, UUID userId) {
        org.springframework.data.jpa.domain.Specification<Post> spec = PostSpecification.filterBy(
                filters.getType(), filters.getCompany(), filters.getRole(), filters.getMinExp(), filters.getMaxExp(), filters.getAuthorId()
        );

        // Custom trending sort
        org.springframework.data.domain.Sort trendingSort = org.springframework.data.domain.Sort.unsorted();
        // Since score is calculated, we can't easily use Sort.by().
        // However, we already have a custom query for trending in the old repo.
        // But the user wants Specification.
        // Let's use a workaround: custom sort in Pageable if supported, or stick to repo query for trending.
        // User said: "Trending -> offset pagination" and "Search -> specification".
        // Let's try to combine. 
        // Actually, Specification with custom order by formula is hard in Spring Data JPA without extra effort.
        // I'll use the repository query for trending but update it to be cleaner.
        
        Page<Post> posts = postRepository.searchByTypeTrending(
            filters.getType(), 
            normalizeTextFilter(filters.getCompany()), 
            normalizeTextFilter(filters.getRole()), 
            filters.getMinExp(), 
            filters.getMaxExp(), 
            pageable
        );
        
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "feed", key = "#userId + '-' + #pageable.pageNumber")
    public CursorPagedResponse<PostDto.FeedResponse> getPersonalizedFeed(Pageable pageable, UUID userId) {
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
                            .map(com.Reffr_Backend.module.feed.entity.PostTag::getTag)
                            .filter(t -> userSkills.contains(t.toLowerCase()))
                            .toList();
                    return PostDto.FeedResponse.from(p, matched);
                })
                .toList();

        return new CursorPagedResponse<>(dtoList, null, posts.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getMyPosts(Instant lastCreatedAt, Pageable pageable, UUID userId) {
        Page<Post> posts;

        if (lastCreatedAt == null) {
            posts = postRepository.findByAuthorFirst(userId, pageable);
        } else {
            posts = postRepository.findByAuthorNext(userId, lastCreatedAt, pageable);
        }
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getUserPosts(UUID authorId, Instant lastCreatedAt, Pageable pageable, UUID currentUserId) {
        if (!userRepository.existsById(authorId)) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found");
        }

        Page<Post> posts;

        if (lastCreatedAt == null) {
            posts = postRepository.findByAuthorFirst(authorId, pageable);
        } else {
            posts = postRepository.findByAuthorNext(authorId, lastCreatedAt, pageable);
        }
        return buildCursorResponse(posts, currentUserId);
    }


    @Override
    @Transactional
    @CacheEvict(value = {"posts", "public_posts", "feed"}, allEntries = true)
    public PostDto.Response updatePost(UUID postId, PostDto.UpdateRequest request, UUID userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCodes.POST_EDIT_DENIED, "You can only edit your own posts");
        }

        if (StringUtils.hasText(request.getContent())) post.setContent(request.getContent().trim());
        if (StringUtils.hasText(request.getCompany())) post.setCompany(request.getCompany().trim());
        if (StringUtils.hasText(request.getCurrentRole())) post.setCurrentRole(request.getCurrentRole().trim());
        if (StringUtils.hasText(request.getLocation())) post.setLocation(request.getLocation().trim());

        if (request.getMinExperience() != null) post.setMinExperience(request.getMinExperience());
        if (request.getMaxExperience() != null) post.setMaxExperience(request.getMaxExperience());

        if (post.getMaxExperience() != null && post.getMinExperience() != null
                && post.getMaxExperience() < post.getMinExperience()) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Maximum experience cannot be less than minimum experience");
        }

        if (request.getTags() != null) {
            post.getTags().clear();
            request.getTags().stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .map(t -> PostTag.builder().post(post).tag(t.trim().toLowerCase()).build())
                    .forEach(post.getTags()::add);
        }

        Post saved = postRepository.save(post);
        log.info("Post updated - id={} user={}", postId, userId);
        return PostDto.Response.from(saved, userId);
    }

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
        log.info("Post deleted - id={} user={}", postId, userId);
    }

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }

    private String normalizeTextFilter(String value) {
        return value == null ? "" : value.trim();
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

    private void notifyFollowersAboutNewPost(Post post) {
        UUID authorId = post.getAuthor().getId();
        List<UUID> followerIds = userFollowRepository.findFollowerIdsByFollowingId(authorId);

        if (followerIds.isEmpty()) {
            return;
        }

        String authorName = post.getAuthor().getName();
        for (UUID followerId : followerIds) {
            if (authorId.equals(followerId)) {
                continue;
            }

            notificationService.send(
                    followerId,
                    NotificationType.NEW_POST,
                    NotificationMessages.newPostTitle(),
                    NotificationMessages.newPostBody(authorName),
                    "POST",
                    post.getId().toString()
            );
        }
    }
}
