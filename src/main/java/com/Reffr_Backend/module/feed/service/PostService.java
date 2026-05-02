package com.Reffr_Backend.module.feed.service;

import com.Reffr_Backend.module.feed.dto.PostDto;
import com.Reffr_Backend.common.dto.CursorPagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface PostService {

    PostDto.Response createPost(PostDto.CreateRequest request, UUID userId);

    PostDto.Response getPost(UUID postId, UUID currentUserId);

    CursorPagedResponse<PostDto.Response> getPosts(PostDto.SearchFilters filters, Pageable pageable, UUID currentUserId);

    CursorPagedResponse<PostDto.FeedResponse> getPersonalizedFeed(Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getFollowingFeed(Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getMyPosts(Instant lastCreatedAt, UUID lastId, Pageable pageable, UUID userId);

    CursorPagedResponse<PostDto.Response> getUserPosts(UUID authorId, Instant lastCreatedAt, UUID lastId, Pageable pageable, UUID currentUserId);

    PostDto.Response updatePost(UUID postId, PostDto.UpdateRequest request, UUID userId);

    void deletePost(UUID postId, UUID userId);

    /**
     * Owner manually marks a post as FULFILLED — for when they got referred externally
     * or through another channel. Closes the post to new volunteers/applicants.
     */
    void markFulfilled(UUID postId, UUID userId);

    /** Owner manually closes a post (prevents new entries, status = CLOSED). */
    void closePost(UUID postId, UUID userId);
}
