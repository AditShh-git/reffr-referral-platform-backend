package com.Reffr_Backend.module.chat.entity;

/**
 * Lifecycle stages tracked inside a Chat.
 *
 * <pre>
 *  ACCEPTED → referral accepted, chat unlocked (initial state for all chats)
 *  REFERRED → referrer submitted the candidate to the company
 *  INACTIVE → no message activity for N days (see app.chat.inactive-after-days)
 * </pre>
 */
public enum ChatWorkflowStatus {
    ACCEPTED,
    REFERRED,
    INACTIVE
}

