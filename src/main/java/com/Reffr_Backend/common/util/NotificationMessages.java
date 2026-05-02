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

    public static String referralVolunteerTitle() {
        return "Someone offered to refer you 👋";
    }

    public static String referralVolunteerBody(String referrerName) {
        return referrerName + " volunteered to refer you — review their profile";
    }

    public static String companyMatchTitle(String company) {
        return "💼 Someone is looking for a referral at " + company;
    }

    public static String companyMatchBody(String company, String role) {
        if (role != null && !role.isBlank()) {
            return "A candidate is seeking a " + role + " referral at " + company + ". You might be able to help!";
        }
        return "A candidate is seeking a referral at " + company + ". You might be able to help!";
    }

    // ── Chat ──────────────────────────────────────────────────────────

    public static String newMessageTitle(String senderName) {
        return "New message from " + senderName;
    }

    public static String newMessageBody(String content) {
        return content.length() > 80 ? content.substring(0, 77) + "..." : content;
    }

    public static String profileViewedTitle() {
        return "Profile viewed";
    }

    public static String profileViewedBody(String viewerName) {
        return (viewerName == null || viewerName.isBlank() ? "Someone" : viewerName) + " viewed your profile";
    }

    public static String newPostTitle() {
        return "New post from someone you follow";
    }

    public static String newPostBody(String authorName) {
        return (authorName == null || authorName.isBlank() ? "Someone" : authorName)
                + " posted a new referral opportunity";
    }
}
