package com.Reffr_Backend.module.feed.service;

import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface PostService {

    PostDto.Response createPost(PostDto.CreateRequest request, UUID userId);

    PostDto.Response getPost(UUID postId, UUID currentUserId);

    CursorPagedResponse<PostDto.Response> getFeed(Instant lastCreatedAt, Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getFeedByType(Post.PostType type, Instant lastCreatedAt, Pageable pageable, UUID userId);

    org.springframework.data.domain.Page<PostDto.FeedResponse> getPersonalizedFeed(Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getMyPosts(Instant lastCreatedAt, Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getUserPosts(UUID authorId, Instant lastCreatedAt, Pageable pageable, UUID currentUserId);

    CursorPagedResponse<PostDto.Response> searchPosts(
            String  company,
            String  role,
            Integer minExp,
            Integer maxExp,
            Instant lastCreatedAt,
            Pageable pageable,
            UUID userId);

    CursorPagedResponse<PostDto.Response> getPosts(
            Post.PostType type,
            String        sort,
            String        company,
            String        role,
            Integer       minExp,
            Integer       maxExp,
            Instant       lastCreatedAt,
            Pageable      pageable,
            UUID          userId);

    PostDto.Response updatePost(UUID postId, PostDto.UpdateRequest request, UUID userId);

    void deletePost(UUID postId, UUID userId);
}