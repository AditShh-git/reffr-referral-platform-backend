package com.Reffr_Backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed binding for the {@code app:} block in application.yaml.
 * Centralises every configurable business rule in one place — no magic constants
 * scattered across service classes.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Referral referral = new Referral();
    private Post     post     = new Post();
    private Chat     chat     = new Chat();
    private Notification notification = new Notification();

    @Getter @Setter
    public static class Referral {
        /** Max referral requests a single user can create per day. */
        private int dailyLimit = 5;
        /** Days before a PENDING referral transitions to EXPIRED. */
        private int expiryDays = 7;
        /** Max volunteers allowed on a REQUEST post before it becomes FULL. */
        private int maxVolunteers = 10;
        /** Max applicants allowed on an OFFER post before it becomes FULL. */
        private int maxApplicants = 20;
    }

    @Getter @Setter
    public static class Post {
        /** Max posts a single user can create per day. */
        private int dailyLimit = 3;
    }

    @Getter @Setter
    public static class Chat {
        /** Days of silence before a chat is flagged INACTIVE. */
        private int inactiveAfterDays = 14;
    }

    @Getter @Setter
    public static class Notification {
        /** Max COMPANY_MATCH notifications sent to a user per company per day. */
        private int companyMatchPerUserPerDay = 3;
    }
}
