package com.Reffr_Backend.module.notification.entity;

public enum NotificationType {

    // Referral flow
    REFERRAL_REQUEST_RECEIVED,  // referrer gets this when someone applies to OFFER post
    REFERRAL_VOLUNTEER,         // seeker gets this when someone volunteers on REQUEST post
    REFERRAL_ACCEPTED,          // seeker/applicant gets this when their referral is accepted
    REFERRAL_REJECTED,          // seeker/applicant gets this when rejected
    REFERRAL_WITHDRAWN,         // referrer gets this when seeker/applicant withdraws
    REFERRAL,                   // generic event for direct connections

    // Chat
    NEW_MESSAGE,                // recipient gets this when a message arrives

    // Feed / follow
    NEW_POST,                   // follower gets this when a followed user creates a post

    // Company match
    COMPANY_MATCH,              // employee gets this when someone posts requesting referral at their company

    // Profile
    PROFILE_VIEWED              // user gets this when someone views their profile
}

