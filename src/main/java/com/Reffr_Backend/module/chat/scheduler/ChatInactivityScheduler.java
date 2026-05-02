package com.Reffr_Backend.module.chat.scheduler;

import com.Reffr_Backend.config.AppProperties;
import com.Reffr_Backend.module.chat.entity.Chat;
import com.Reffr_Backend.module.chat.entity.ChatWorkflowStatus;
import com.Reffr_Backend.module.chat.repository.ChatRepository;
import com.Reffr_Backend.module.notification.entity.NotificationType;
import com.Reffr_Backend.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flags accepted chats as INACTIVE when there has been no message activity
 * for {@code app.chat.inactive-after-days} days.
 *
 * <p><strong>Critical guard</strong>: Only ACCEPTED chats are targeted.
 * REFERRED chats are intentionally skipped — a successful referral conversation
 * should never be flagged as inactive just because the referral was already submitted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatInactivityScheduler {

    private final ChatRepository     chatRepository;
    private final NotificationService notificationService;
    private final AppProperties      appProperties;

    @Scheduled(cron = "0 0 3 * * *")  // 3 AM daily
    @Transactional
    public void flagInactiveChats() {
        int inactiveDays = appProperties.getChat().getInactiveAfterDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(inactiveDays);

        // Only ACCEPTED — never touches REFERRED chats
        List<Chat> staleChats = chatRepository.findInactiveAcceptedChats(cutoff);

        if (staleChats.isEmpty()) {
            log.debug("ChatInactivityScheduler: no inactive chats found");
            return;
        }

        for (Chat chat : staleChats) {
            chat.setWorkflowStatus(ChatWorkflowStatus.INACTIVE);

            // Notify both participants
            notificationService.send(
                    chat.getSeeker().getId(),
                    NotificationType.NEW_MESSAGE,  // closest fit; can add CHAT_INACTIVE type later
                    "Chat marked inactive",
                    "Your referral chat has been inactive for " + inactiveDays + " days",
                    "CHAT",
                    chat.getId().toString()
            );
            notificationService.send(
                    chat.getReferrer().getId(),
                    NotificationType.NEW_MESSAGE,
                    "Chat marked inactive",
                    "Your referral chat has been inactive for " + inactiveDays + " days",
                    "CHAT",
                    chat.getId().toString()
            );
        }

        log.info("ChatInactivityScheduler: flagged {} chats as INACTIVE (cutoff={})",
                staleChats.size(), cutoff);
    }
}
