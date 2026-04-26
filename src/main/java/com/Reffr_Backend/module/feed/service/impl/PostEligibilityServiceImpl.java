package com.Reffr_Backend.module.feed.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.module.feed.entity.Post;
import com.Reffr_Backend.module.feed.service.PostEligibilityService;
import com.Reffr_Backend.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostEligibilityServiceImpl implements PostEligibilityService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "post_rate_limit:";
    private static final int MAX_POSTS_PER_HOUR = 5;

    @Override
    public void validateCreatePost(User user, Post.PostType type) {
        // ── Step 1: Base Onboarding check ────────────────────────────────
        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCodes.ONBOARDING_REQUIRED, "Please complete your onboarding before posting");
        }

        // ── Step 2: Type-specific rules ──────────────────────────────────
        if (type == Post.PostType.REQUEST) {
            validateRequestEligibility(user);
        } else if (type == Post.PostType.OFFER) {
            validateOfferEligibility(user);
        }

        // ── Step 3: Rate limiting ────────────────────────────────────────
        checkRateLimit(user);
    }

    private void validateRequestEligibility(User user) {
        // REQUEST → anyone with basic profile
        if (user.getSkills().size() < 3) {
            throw new BusinessException(ErrorCodes.NOT_ELIGIBLE, "You need at least 3 skills to post a referral request");
        }
        if (user.getPrimaryEmail() == null || user.getPrimaryEmail().isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_EMAIL, "Valid email is required to post");
        }
    }

    private void validateOfferEligibility(User user) {
        // OFFER → only trusted / verified users
        if (!user.isVerified()) {
            throw new BusinessException(ErrorCodes.NOT_ELIGIBLE, "You must verify your company before offering referrals");
        }
        if (!user.hasResume()) {
            throw new BusinessException(ErrorCodes.RESUME_NOT_FOUND, "Please upload your resume to build trust before offering referrals");
        }
    }

    private void checkRateLimit(User user) {
        String key = RATE_LIMIT_PREFIX + user.getId();
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        if (count != null && count > MAX_POSTS_PER_HOUR) {
            throw new BusinessException(ErrorCodes.RATE_LIMIT_EXCEEDED, "Post limit exceeded. You can create max " + MAX_POSTS_PER_HOUR + " posts per hour.");
        }
    }
}
