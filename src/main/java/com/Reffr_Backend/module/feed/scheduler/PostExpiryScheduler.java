package com.Reffr_Backend.module.feed.scheduler;

import com.Reffr_Backend.module.feed.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostExpiryScheduler {

    private final PostRepository postRepository;

    @Scheduled(cron = "0 0 * * * *") // Run every hour at the top of the hour
    @Transactional
    @CacheEvict(value = "feed", allEntries = true)
    public void runPostExpiry() {
        log.info("Running post expiry scheduler...");
        try {
            int updatedCount = postRepository.expireOpenPosts();
            if (updatedCount > 0) {
                log.info("Successfully marked {} posts as EXPIRED.", updatedCount);
            } else {
                log.info("No expired posts found to update.");
            }
        } catch (Exception e) {
            log.error("Failed to execute post expiry scheduler", e);
        }
    }
}
