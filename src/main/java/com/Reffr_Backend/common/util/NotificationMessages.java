package com.Reffr_Backend.common.util;

public final class NotificationMessages {

    private NotificationMessages() {}

    // ── Referral ──────────────────────────────────────────────────────

    public static String referralRequestTitle() {
        return "New referral request";
    }

    public static String referralRequestBody(String role, String company) {
        if (role != null && company != null) return "Someone requested a referral for " + role + " at " + company;
        if (role != null)    return "Someone requested a referral for " + role;
        if (company != null) return "Someone requested a referral at " + company;
        return "You have a new referral request";
    }

    public static String referralAcceptedTitle() {
        return "Referral accepted 🎉";
    }

    public static String referralAcceptedBody(String referrerName) {
        return referrerName + " accepted your request — chat is now open";
    }

    public static String referralRejectedTitle() {
        return "Referral request not accepted";
    }

    public static String referralRejectedBody(String referrerName) {
        return referrerName + " was unable to accept your referral request";
    }

    public static String referralWithdrawnTitle() {
        return "Referral request withdrawn";
    }

    public static String referralWithdrawnBody(String requesterName) {
        return requesterName + " withdrew their referral request";
    }

    // ── Chat ──────────────────────────────────────────────────────────

    public static String newMessageTitle(String senderName) {
        return "New message from " + senderName;
    }

    public static String newMessageBody(String content) {
        return content.length() > 80 ? content.substring(0, 77) + "..." : content;
    }
}
