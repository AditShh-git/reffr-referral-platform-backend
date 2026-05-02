package com.Reffr_Backend.module.feed.entity;

/**
 * Controls who can see a seeker's resume link on a REQUEST post.
 *
 * <ul>
 *   <li>{@code PUBLIC}        — resume URL visible to all authenticated users</li>
 *   <li>{@code VERIFIED_ONLY} — resume URL visible only to company-verified users;
 *                               field is nulled-out in the response for others</li>
 *   <li>{@code PRIVATE}       — resume never exposed via post; seeker shares
 *                               directly in chat after acceptance</li>
 * </ul>
 */
public enum PostVisibility {
    PUBLIC,
    VERIFIED_ONLY,
    PRIVATE
}
