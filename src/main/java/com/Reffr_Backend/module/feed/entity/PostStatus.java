package com.Reffr_Backend.module.feed.entity;

/**
 * Lifecycle status of a Post.
 * Replaces the old {@code isClosed} boolean — gives much finer control
 * over post state for frontend rendering and business rules.
 *
 * <pre>
 *  OPEN      → accepting new volunteers / applicants
 *  FULL      → limit (maxVolunteers / maxApplicants) reached; no new entries
 *  CLOSED    → manually closed by the post author
 *  EXPIRED   → automatic: expiresAt passed with no accepted referral
 *  FULFILLED → a referral was accepted and chat was opened (success state)
 * </pre>
 */
public enum PostStatus {
    OPEN,
    FULL,
    CLOSED,
    EXPIRED,
    FULFILLED
}
