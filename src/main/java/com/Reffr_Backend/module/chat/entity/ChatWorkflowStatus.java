package com.Reffr_Backend.module.chat.entity;

/**
 * Referral workflow lifecycle tracked inside the Chat.
 * REQUESTED → chat peer opened (initial state)
 * ACCEPTED  → referral accepted, chat unlocked
 * REFERRED  → referrer has submitted the candidate to the company
 */
public enum ChatWorkflowStatus {
    REQUESTED,
    ACCEPTED,
    REFERRED
}
