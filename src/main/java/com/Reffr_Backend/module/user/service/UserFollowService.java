package com.Reffr_Backend.module.user.service;

import java.util.UUID;

public interface UserFollowService {

    void follow(UUID currentUserId, UUID targetUserId);

    void unfollow(UUID currentUserId, UUID targetUserId);
}
