package com.Reffr_Backend.module.notification.service;

import com.Reffr_Backend.module.user.entity.User;

import java.util.UUID;

public interface EmailService {

    /** Low-level dispatch — used internally and for testing. */
    void sendSimpleMessage(String to, String subject, String text);

    /** High-level typed event — enforces throttle + preferences. */
    void sendReferralAccepted(User recipient, String referrerName, UUID chatId);

    void sendReferralRejected(User recipient, String referrerName);

    void sendNewMessage(User recipient, String senderName, String messagePreview, UUID chatId);
    
    /** Triggered by scheduler to retry failing transactions. */
    void retryFailedEmails();
}
