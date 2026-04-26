package com.Reffr_Backend.module.notification.entity;

public enum NotificationType {

    // Referral flow
    REFERRAL_REQUEST_RECEIVED,  // referrer gets this when someone applies
    REFERRAL_ACCEPTED,          // seeker gets this when referrer accepts
    REFERRAL_REJECTED,          // seeker gets this when referrer rejects
    REFERRAL_WITHDRAWN,         // referrer gets this when seeker withdraws

    // Chat
    NEW_MESSAGE,                // recipient gets this when a message arrives

    // Feed / follow
    NEW_POST,                   // follower gets this when a followed user creates a post

    // Profile
    PROFILE_VIEWED              // user gets this when someone views their profile
}
