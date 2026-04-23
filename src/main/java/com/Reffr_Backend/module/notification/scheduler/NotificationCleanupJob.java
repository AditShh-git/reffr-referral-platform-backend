package com.Reffr_Backend.module.notification.scheduler;

import com.Reffr_Backend.module.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    /**
     * Runs every day at 3:00 AM.
     * Deletes read notifications older than 30 days.
     * Unread notifications are never deleted automatically.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteOldReadNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteOldRead(cutoff);
        if (deleted > 0) {
            log.info("Notification cleanup — deleted {} old read notifications", deleted);
        }
    }
}
