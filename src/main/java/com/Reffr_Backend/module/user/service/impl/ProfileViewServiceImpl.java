package com.Reffr_Backend.module.user.service.impl;

import com.Reffr_Backend.common.util.NotificationMessages;
import com.Reffr_Backend.module.notification.entity.Notification;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.repository.NotificationRepository;
import com.Reffr_Backend.module.notification.service.NotificationService;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import com.Reffr_Backend.module.user.service.ProfileViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileViewServiceImpl implements ProfileViewService {

    private static final String ENTITY_TYPE = "PROFILE_VIEW";
    private static final String RECENT_VIEWERS_KEY = "profile_recent_viewers:";
    private static final int RECENT_VIEWERS_LIMIT = 5;

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    @Override
    public void recordProfileView(UUID viewerId, String viewerName, UUID viewedUserId) {
        if (viewerId == null || viewedUserId == null || viewerId.equals(viewedUserId)) {
            return;
        }

        updateRecentViewers(viewerId, viewedUserId);

        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        boolean alreadyNotified = notificationRepository
                .existsByUserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtAfter(
                        viewedUserId,
                        NotificationType.PROFILE_VIEWED,
                        ENTITY_TYPE,
                        viewerId.toString(),
                        threshold
                );

        if (alreadyNotified) {
            log.debug("Profile view notification throttled viewer={} viewed={}", viewerId, viewedUserId);
            return;
        }

        notificationService.send(
                viewedUserId,
                NotificationType.PROFILE_VIEWED,
                NotificationMessages.profileViewedTitle(),
                NotificationMessages.profileViewedBody(viewerName),
                ENTITY_TYPE,
                viewerId.toString()
        );
    }

    @Override
    public UserDto.ProfileViewHistoryResponse getProfileViews(UUID userId) {
        List<Notification> notifications = notificationRepository.findProfileViewNotifications(
                userId,
                PageRequest.of(0, RECENT_VIEWERS_LIMIT)
        );

        long totalViews = notificationRepository.countByUserIdAndType(userId, NotificationType.PROFILE_VIEWED);

        List<UUID> viewerIds = notifications.stream()
                .map(Notification::getEntityId)
                .map(UUID::fromString)
                .toList();

        Map<UUID, User> usersById = new LinkedHashMap<>();
        userRepository.findAllById(viewerIds).forEach(user -> usersById.put(user.getId(), user));

        List<UserDto.ProfileViewItem> viewers = notifications.stream()
                .map(notification -> {
                    UUID viewerId = UUID.fromString(notification.getEntityId());
                    User viewer = usersById.get(viewerId);
                    if (viewer == null) {
                        return null;
                    }
                    return UserDto.ProfileViewItem.from(
                            viewer,
                            notification.getCreatedAt().toInstant(ZoneOffset.UTC)
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return UserDto.ProfileViewHistoryResponse.builder()
                .viewers(viewers)
                .limited(totalViews > RECENT_VIEWERS_LIMIT)
                .limit(RECENT_VIEWERS_LIMIT)
                .totalViews(totalViews)
                .build();
    }

    private void updateRecentViewers(UUID viewerId, UUID viewedUserId) {
        String key = RECENT_VIEWERS_KEY + viewedUserId;
        String viewer = viewerId.toString();

        redisTemplate.opsForList().remove(key, 0, viewer);
        redisTemplate.opsForList().leftPush(key, viewer);
        redisTemplate.opsForList().trim(key, 0, RECENT_VIEWERS_LIMIT - 1);
    }
}
