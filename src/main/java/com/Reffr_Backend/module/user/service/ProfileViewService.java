package com.Reffr_Backend.module.user.service;

import java.util.UUID;

public interface ProfileViewService {

    void recordProfileView(UUID viewerId, String viewerName, UUID viewedUserId);

    com.Reffr_Backend.module.user.dto.UserDto.ProfileViewHistoryResponse getProfileViews(UUID userId);
}
