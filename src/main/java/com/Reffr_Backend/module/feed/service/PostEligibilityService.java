package com.Reffr_Backend.module.feed.service;

import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.user.entity.User;

public interface PostEligibilityService {
    /**
     * Validates if a user is eligible to create a post of a specific type.
     * Throws BusinessException if ineligible.
     */
    void validateCreatePost(User user, Post.PostType type);
}
