package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.exception.BusinessException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.entity.UserFollow;
import com.Reffr_Backend.module.user.repository.UserFollowRepository;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;

    @Override
    @Transactional
    public void follow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "You cannot follow yourself");
        }

        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));

        if (userFollowRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)) {
            log.debug("Follow ignored - already followed follower={} following={}", currentUserId, targetUserId);
            return;
        }

        userFollowRepository.save(UserFollow.builder()
                .follower(follower)
                .following(following)
                .build());

        log.info("User followed follower={} following={}", currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void unfollow(UUID currentUserId, UUID targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCodes.INVALID_REQUEST, "You cannot unfollow yourself");
        }

        if (!userRepository.existsById(targetUserId)) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found");
        }

        long deleted = userFollowRepository.deleteByFollowerIdAndFollowingId(currentUserId, targetUserId);
        if (deleted == 0) {
            log.debug("Unfollow ignored - relation not found follower={} following={}", currentUserId, targetUserId);
            return;
        }

        log.info("User unfollowed follower={} following={}", currentUserId, targetUserId);
    }
}
