package com.Reffr_Backend.module.notification.scheduler;

import com.Reffr_Backend.module.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decoupled scheduler to trigger email retries.
 * This resolves JDK proxy issues by not having @Scheduled on methods 
 * not present in the interface, while keeping EmailServiceImpl clean.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {

    private final EmailService emailService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void retryFailedEmails() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Email retry already running, skipping this cycle");
            return;
        }

        long start = System.currentTimeMillis();

        try {
            emailService.retryFailedEmails();
            log.info("Email retry completed in {} ms",
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Email retry scheduler failed", e);
        } finally {
            running.set(false);
        }
    }
}