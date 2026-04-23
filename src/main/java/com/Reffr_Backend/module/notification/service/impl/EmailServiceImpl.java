package com.Reffr_Backend.module.notification.service.impl;

import com.Reffr_Backend.module.notification.entity.FailedEmail;
import com.Reffr_Backend.module.notification.repository.FailedEmailRepository;
import com.Reffr_Backend.module.notification.service.EmailService;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final int    THROTTLE_MINUTES  = 5;
    private static final int    MAX_PREVIEW_LENGTH = 200;

    private final JavaMailSender        javaMailSender;
    private final FailedEmailRepository failedEmailRepository;
    private final UserRepository        userRepository;

    @Value("${spring.mail.username:noreply@reffr.com}")
    private String fromEmail;

    @Value("${reffr.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // ── Typed high-level events ────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendReferralAccepted(User recipient, String referrerName, UUID chatId) {
        if (!canSendReferralEmail(recipient)) return;

        String subject = "\uD83C\uDF89 Your referral request was accepted";
        String body = String.format("""
                Hi %s,

                Great news — your referral request has been accepted by %s.

                You can now chat directly and proceed further.

                \u2192 Open chat: %s/chat/%s

                Good luck \uD83D\uDE80
                """, recipient.getName(), referrerName, frontendUrl, chatId);

        dispatch(recipient, subject, body, EmailType.REFERRAL);
    }

    @Override
    @Async("notificationExecutor")
    public void sendReferralRejected(User recipient, String referrerName) {
        if (!canSendReferralEmail(recipient)) return;

        String subject = "Referral Update";
        String body = String.format("""
                Hi %s,

                Unfortunately, your referral request was not accepted by %s at this time.

                Don't be discouraged — keep exploring opportunities on Reffr.
                """, recipient.getName(), referrerName);

        dispatch(recipient, subject, body, EmailType.REFERRAL);
    }

    /**
     * @param senderCompany pass null / blank if not applicable — rendered only when present
     */
    @Override
    @Async("notificationExecutor")
    public void sendNewMessage(User recipient, String senderName, String messagePreview, UUID chatId) {
        if (!canSendChatEmail(recipient)) return;

        String safePreview = sanitize(messagePreview);
        String subject = "\uD83D\uDCAC New message from " + senderName;
        String body = String.format("""
                Hi %s,

                You received a new message from %s:

                "%s"

                \u2192 Reply here: %s/chat/%s
                """, recipient.getName(), senderName, safePreview, frontendUrl, chatId);

        dispatch(recipient, subject, body, EmailType.CHAT);
    }

    // ── Low-level primitive (direct use / tests) ───────────────────────

    @Override
    @Async("notificationExecutor")
    public void sendSimpleMessage(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            log.warn("Email skipped — recipient is null or blank");
            return;
        }
        doSend(to, subject, text);
    }

    // ── Retry mechanism ───────────────────────────────────────────────
    
    @Override
    public void retryFailedEmails() {
        List<FailedEmail> failures = failedEmailRepository.findTop10ByOrderByCreatedAtAsc();
        if (failures.isEmpty()) return;

        log.info("Retrying {} failed email(s)", failures.size());

        for (FailedEmail f : failures) {
            if (f.getRetryCount() >= 5) {
                log.warn("Dropping dead-letter email after 5 retries — to={} subject=\"{}\"", f.getTo(), f.getSubject());
                failedEmailRepository.delete(f);
                continue;
            }
            try {
                doSend(f.getTo(), f.getSubject(), f.getBody());
                failedEmailRepository.delete(f);
            } catch (Exception e) {
                f.setRetryCount(f.getRetryCount() + 1);
                failedEmailRepository.save(f);
                log.warn("Retry {}/5 still failing — to={} error={}", f.getRetryCount(), f.getTo(), e.getMessage());
            }
        }
    }

    // ── Per-type throttle guards ───────────────────────────────────────

    private boolean canSendReferralEmail(User user) {
        if (!meetsBaseRequirements(user)) return false;
        Instant threshold = Instant.now().minusSeconds(THROTTLE_MINUTES * 60L);
        if (user.getLastReferralEmailAt() != null && user.getLastReferralEmailAt().isAfter(threshold)) {
            log.debug("Referral email throttled for userId={}", user.getId());
            return false;
        }
        return true;
    }

    private boolean canSendChatEmail(User user) {
        if (!meetsBaseRequirements(user)) return false;
        Instant threshold = Instant.now().minusSeconds(THROTTLE_MINUTES * 60L);
        if (user.getLastChatEmailAt() != null && user.getLastChatEmailAt().isAfter(threshold)) {
            log.debug("Chat email throttled for userId={}", user.getId());
            return false;
        }
        return true;
    }

    private boolean meetsBaseRequirements(User user) {
        if (user.getPrimaryEmail() == null || user.getPrimaryEmail().isBlank()) {
            log.debug("Email skipped — no address for userId={}", user.getId());
            return false;
        }
        if (!user.getPrimaryEmail().contains("@")) {
            log.warn("Email skipped — invalid address '{}' for userId={}", user.getPrimaryEmail(), user.getId());
            return false;
        }
        if (!user.isEmailNotificationsEnabled()) {
            log.debug("Email skipped — notifications disabled for userId={}", user.getId());
            return false;
        }
        return true;
    }

    // ── Internal helpers ───────────────────────────────────────────────

    private enum EmailType { CHAT, REFERRAL }

    private void dispatch(User recipient, String subject, String body, EmailType type) {
        doSend(recipient.getPrimaryEmail(), subject, body);
        // Update per-type anchor (best-effort — no tx required)
        if (type == EmailType.CHAT) {
            recipient.setLastChatEmailAt(Instant.now());
        } else {
            recipient.setLastReferralEmailAt(Instant.now());
        }
        userRepository.save(recipient);
    }

    private void doSend(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text != null ? text : "");
            javaMailSender.send(message);
            log.info("Email sent to={} subject=\"{}\"", to, subject);
        } catch (Exception e) {
            log.error("Email delivery failed to={} subject=\"{}\" error={}", to, subject, e.getMessage(), e);
            recordFailure(to, subject, text, e.getMessage());
        }
    }

    private void recordFailure(String to, String subject, String body, String error) {
        try {
            failedEmailRepository.save(FailedEmail.builder()
                    .to(to)
                    .subject(subject)
                    .body(body)
                    .errorMessage(error != null ? error.substring(0, Math.min(error.length(), 1000)) : "unknown")
                    .build());
        } catch (Exception ex) {
            log.error("Could not persist failed-email record for to={}", to, ex);
        }
    }

    /** Collapses whitespace/line-breaks and truncates to MAX_PREVIEW_LENGTH. */
    private String sanitize(String input) {
        if (input == null) return "";
        String collapsed = input.replaceAll("\\s+", " ").trim();
        return collapsed.length() > MAX_PREVIEW_LENGTH
                ? collapsed.substring(0, MAX_PREVIEW_LENGTH) + "..."
                : collapsed;
    }
}
