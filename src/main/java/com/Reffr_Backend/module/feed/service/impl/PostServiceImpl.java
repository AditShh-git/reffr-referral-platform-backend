package com.Reffr_Backend.module.feed.service.impl;

import com.Reffr_Backend.common.dto.CursorPagedResponse;
import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.common.util.SanitizerUtils;
import com.Reffr_Backend.config.AppProperties;
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
import com.Reffr_Backend.module.user.infrastructure.ResumeSnapshotService;
import com.Reffr_Backend.module.user.repository.UserFollowRepository;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final AppProperties appProperties;
    private final StringRedisTemplate redisTemplate;
    private final ResumeSnapshotService resumeSnapshotService;

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

        String resumeKey = null;
        if (request.getType() == Post.PostType.REQUEST && user.hasResume()) {
            resumeKey = user.getResumeS3Key();
        }

        Post post = Post.builder()
                .author(user)
                .type(request.getType())
                .title(SanitizerUtils.sanitize(request.getTitle()))
                .content(SanitizerUtils.sanitize(request.getContent()))
                .company(SanitizerUtils.sanitize(request.getCompany()))
                .currentRole(SanitizerUtils.sanitize(request.getCurrentRole()))
                .location(SanitizerUtils.sanitize(request.getLocation()))
                .minExperience(request.getMinExperience())
                .maxExperience(request.getMaxExperience())
                .githubLink(SanitizerUtils.sanitize(request.getGithubLink()))
                .linkedinLink(SanitizerUtils.sanitize(request.getLinkedinLink()))
                .resumeVisibility(request.getResumeVisibility() != null
                        ? request.getResumeVisibility()
                        : com.Reffr_Backend.module.feed.entity.PostVisibility.PUBLIC)
                .resumeSnapshotKey(resumeKey)
                .urgencyDeadline(request.getUrgencyDeadline())
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
        notifyCompanyMatchUsers(saved);

        log.info("Post created - id={} type={} user={}",
                saved.getId(), saved.getType(),
                user.getGithubUsername() != null ? user.getGithubUsername() : "unknown");

        return resolveResponse(saved, userId, true);
    }

    @Override
    @Transactional
    public PostDto.Response getPost(UUID postId, UUID currentUserId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));

        postRepository.incrementViews(postId);
        boolean viewerVerified = isViewerVerified(currentUserId);
        return resolveResponse(post, currentUserId, viewerVerified);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getPosts(PostDto.SearchFilters filters, Pageable pageable, UUID currentUserId) {
        if ("trending".equalsIgnoreCase(filters.getSort())) {
            return getTrendingPosts(filters, pageable, currentUserId);
        }
        
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
        UUID cursorId = filters.getCursorId();
        Post.PostType type = filters.getType();

        if (type == null) {
            posts = (cursor == null || cursorId == null)
                    ? postRepository.findFeedFirst(pageable)
                    : postRepository.findFeedNext(cursor, cursorId, pageable);
        } else {
            posts = (cursor == null || cursorId == null)
                    ? postRepository.findFeedByTypeFirst(type, pageable)
                    : postRepository.findFeedByTypeNext(type, cursor, cursorId, pageable);
        }
        return buildCursorResponse(posts, userId);
    }

    private CursorPagedResponse<PostDto.Response> getLatestPostsWithSpec(PostDto.SearchFilters filters, Pageable pageable, UUID userId) {
        org.springframework.data.jpa.domain.Specification<Post> spec = PostSpecification.filterBy(
                filters.getType(), filters.getCompany(), filters.getRole(), filters.getMinExp(), filters.getMaxExp(), filters.getAuthorId()
        );
        
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                org.springframework.data.domain.Sort.by("createdAt").descending().and(org.springframework.data.domain.Sort.by("id").descending())
            );
        }

        Page<Post> posts = postRepository.findAll(spec, sortedPageable);
        return buildCursorResponse(posts, userId);
    }

    private CursorPagedResponse<PostDto.Response> getTrendingPosts(PostDto.SearchFilters filters, Pageable pageable, UUID userId) {
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
                .map(s -> s.getSkillName().toLowerCase())
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

        return new CursorPagedResponse<>(dtoList, null, null, posts.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getFollowingFeed(Pageable pageable, UUID userId) {
        if (!userFollowRepository.existsByFollowerId(userId)) {
            return new CursorPagedResponse<>(List.of(), null, null, false);
        }
        Page<Post> posts = postRepository.findByFollowedUsers(userId, pageable);
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getMyPosts(Instant lastCreatedAt, UUID lastId, Pageable pageable, UUID userId) {
        Page<Post> posts;
        if (lastCreatedAt == null || lastId == null) {
            posts = postRepository.findByAuthorFirst(userId, pageable);
        } else {
            posts = postRepository.findByAuthorNext(userId, lastCreatedAt, lastId, pageable);
        }
        return buildCursorResponse(posts, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPagedResponse<PostDto.Response> getUserPosts(UUID authorId, Instant lastCreatedAt, UUID lastId, Pageable pageable, UUID currentUserId) {
        if (!userRepository.existsById(authorId)) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found");
        }
        Page<Post> posts;
        if (lastCreatedAt == null || lastId == null) {
            posts = postRepository.findByAuthorFirst(authorId, pageable);
        } else {
            posts = postRepository.findByAuthorNext(authorId, lastCreatedAt, lastId, pageable);
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

        if (StringUtils.hasText(request.getTitle()))       post.setTitle(request.getTitle().trim());
        if (StringUtils.hasText(request.getContent()))     post.setContent(request.getContent().trim());
        if (StringUtils.hasText(request.getCompany()))     post.setCompany(request.getCompany().trim());
        if (StringUtils.hasText(request.getCurrentRole())) post.setCurrentRole(request.getCurrentRole().trim());
        if (StringUtils.hasText(request.getLocation()))    post.setLocation(request.getLocation().trim());
        if (StringUtils.hasText(request.getGithubLink()))  post.setGithubLink(request.getGithubLink().trim());
        if (StringUtils.hasText(request.getLinkedinLink())) post.setLinkedinLink(request.getLinkedinLink().trim());
        if (request.getResumeVisibility() != null)  post.setResumeVisibility(request.getResumeVisibility());
        if (request.getUrgencyDeadline() != null)   post.setUrgencyDeadline(request.getUrgencyDeadline());
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
        return resolveResponse(saved, userId, true);
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

    private PostDto.Response resolveResponse(Post post, UUID viewerId, boolean viewerVerified) {
        String resumeUrl = null;
        if (post.getType() == Post.PostType.REQUEST && post.getResumeSnapshotKey() != null) {
            resumeUrl = "/api/v1/resumes/" + post.getId();
        }
        return PostDto.Response.from(post, viewerId, viewerVerified, resumeUrl);
    }

    private boolean isViewerVerified(UUID viewerId) {
        if (viewerId == null) return false;
        return userRepository.findById(viewerId)
                .map(com.Reffr_Backend.module.user.entity.User::isVerified)
                .orElse(false);
    }

    private String normalizeTextFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private CursorPagedResponse<PostDto.Response> buildCursorResponse(Page<Post> posts, UUID userId) {
        boolean viewerVerified = isViewerVerified(userId);
        List<PostDto.Response> dtoList = posts.stream()
                .map(p -> resolveResponse(p, userId, viewerVerified))
                .toList();

        Instant nextCursor = null;
        UUID nextCursorId = null;
        if (!posts.isEmpty() && posts.hasNext()) {
            Post lastPost = posts.getContent().get(posts.getContent().size() - 1);
            nextCursor = lastPost.getCreatedAt();
            nextCursorId = lastPost.getId();
        }
        return new CursorPagedResponse<>(dtoList, nextCursor, nextCursorId, posts.hasNext());
    }

    private void notifyFollowersAboutNewPost(Post post) {
        UUID authorId = post.getAuthor().getId();
        List<UUID> followerIds = userFollowRepository.findFollowerIdsByFollowingId(authorId);
        String authorName = post.getAuthor().getName();
        for (UUID followerId : followerIds) {
            if (authorId.equals(followerId)) continue;
            notificationService.send(followerId, NotificationType.NEW_POST, NotificationMessages.newPostTitle(), NotificationMessages.newPostBody(authorName), "POST", post.getId().toString());
        }
    }

    private void notifyCompanyMatchUsers(Post post) {
        if (post.getType() != Post.PostType.REQUEST) return;
        if (post.getTags() == null || post.getTags().isEmpty()) return;

        UUID authorId = post.getAuthor().getId();
        int maxPerDay = appProperties.getNotification().getCompanyMatchPerUserPerDay();
        String today  = java.time.LocalDate.now().toString();

        post.getTags().stream()
                .map(t -> t.getTag())
                .distinct()
                .forEach(tag -> {
                    List<UUID> employeeIds = userRepository.findIdsByCurrentCompanyIgnoreCase(tag);
                    for (UUID employeeId : employeeIds) {
                        if (authorId.equals(employeeId)) continue;
                        String redisKey = "company_notif:" + employeeId + ":" + tag + ":" + today;
                        Long count = redisTemplate.opsForValue().increment(redisKey);
                        if (count == 1) redisTemplate.expire(redisKey, java.time.Duration.ofDays(1));
                        if (count > maxPerDay) continue;
                        notificationService.send(employeeId, NotificationType.COMPANY_MATCH, NotificationMessages.companyMatchTitle(tag), NotificationMessages.companyMatchBody(tag, post.getCurrentRole()), "POST", post.getId().toString());
                    }
                });
    }

    @Override
    @Transactional
    public void markFulfilled(UUID postId, UUID userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));
        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCodes.POST_EDIT_DENIED, "Only the post author can mark it fulfilled");
        }
        post.markFulfilled();
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void closePost(UUID postId, UUID userId) {
        Post post = postRepository.findActiveById(postId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "Post not found"));
        if (!post.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCodes.POST_EDIT_DENIED, "Only the post author can close this post");
        }
        post.close();
        postRepository.save(post);
    }
}
