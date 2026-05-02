package com.Reffr_Backend.common.exception;

/**
 * Global machine-readable error codes for the Reffr platform.
 * Used by all exceptions in the BaseException hierarchy.
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // ── Authentication ────────────────────────────────────────────────
    public static final String AUTH_REQUIRED         = "AUTH_REQUIRED";
    public static final String ACCESS_DENIED         = "ACCESS_DENIED";
    public static final String UNAUTHORIZED          = "UNAUTHORIZED";
    public static final String TOKEN_EXPIRED        = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID        = "TOKEN_INVALID";

    // ── User / Onboarding ─────────────────────────────────────────────
    public static final String USER_NOT_FOUND       = "USER_NOT_FOUND";
    public static final String RESUME_NOT_FOUND     = "RESUME_NOT_FOUND";
    public static final String INCOMPLETE_PROFILE   = "INCOMPLETE_PROFILE";
    public static final String ONBOARDING_REQUIRED  = "ONBOARDING_REQUIRED";
    public static final String INVALID_EMAIL        = "INVALID_EMAIL";
    public static final String OTP_EXPIRED          = "OTP_EXPIRED";
    public static final String INVALID_OTP          = "INVALID_OTP";
    public static final String NO_VERIFICATION_EMAIL = "NO_VERIFICATION_EMAIL";
    public static final String EMAIL_UPDATE_REQUIRED = "EMAIL_UPDATE_REQUIRED";

    // ── Posts / Feed ──────────────────────────────────────────────────
    public static final String POST_NOT_FOUND       = "POST_NOT_FOUND";
    public static final String POST_EDIT_DENIED      = "POST_EDIT_DENIED";
    public static final String POST_DELETE_DENIED    = "POST_DELETE_DENIED";
    public static final String VERIFICATION_REQUIRED = "VERIFICATION_REQUIRED";

    // ── Referrals ─────────────────────────────────────────────────────
    public static final String REFERRAL_NOT_FOUND   = "REFERRAL_NOT_FOUND";
    public static final String SELF_REFERRAL        = "SELF_REFERRAL";
    public static final String DUPLICATE_REQUEST    = "DUPLICATE_REQUEST";
    public static final String INVALID_STATE        = "INVALID_STATE";
    public static final String FEEDBACK_NOT_ALLOWED = "FEEDBACK_NOT_ALLOWED";
    public static final String DUPLICATE_FEEDBACK   = "DUPLICATE_FEEDBACK";
    public static final String REFERRAL_EXPIRED     = "REFERRAL_EXPIRED";

    // ── Posts / Feed (extended) ──────────────────────────────────────────
    public static final String POST_CLOSED           = "POST_CLOSED";
    public static final String MAX_VOLUNTEERS_REACHED = "MAX_VOLUNTEERS_REACHED";
    public static final String MAX_APPLICANTS_REACHED = "MAX_APPLICANTS_REACHED";
    public static final String POST_QUESTION_NOT_FOUND = "POST_QUESTION_NOT_FOUND";
    public static final String CANNOT_ASK_OWN_POST   = "CANNOT_ASK_OWN_POST";

    // ── Chat ──────────────────────────────────────────────────────────
    public static final String CHAT_NOT_FOUND       = "CHAT_NOT_FOUND";
    public static final String CHAT_LOCKED          = "CHAT_LOCKED";
    public static final String INVALID_MESSAGE       = "INVALID_MESSAGE";
    public static final String INVALID_WORKFLOW_STATE = "INVALID_WORKFLOW_STATE";

    // ── System ────────────────────────────────────────────────────────
    public static final String INVALID_REQUEST      = "INVALID_REQUEST";
    public static final String GENERIC_ERROR        = "GENERIC_ERROR";
    public static final String CONCURRENCY_CONFLICT = "CONCURRENCY_CONFLICT";
    public static final String FILE_TOO_LARGE       = "FILE_TOO_LARGE";
    public static final String OTP_LIMIT_EXCEEDED   = "OTP_LIMIT_EXCEEDED";
    public static final String DUPLICATE_ACCEPTED   = "DUPLICATE_ACCEPTED";
    public static final String NOT_ELIGIBLE         = "NOT_ELIGIBLE";
    public static final String RATE_LIMIT_EXCEEDED  = "RATE_LIMIT_EXCEEDED";
}
