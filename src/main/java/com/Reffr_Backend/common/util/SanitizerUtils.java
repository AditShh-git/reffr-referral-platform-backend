package com.Reffr_Backend.common.util;

import org.springframework.util.StringUtils;

public final class SanitizerUtils {

    private SanitizerUtils() {}

    /**
     * Basic sanitization: trim + strip basic HTML tags to prevent simple XSS/abuse.
     * In a real app, use a library like OWASP Java HTML Sanitizer.
     */
    public static String sanitize(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        // Trim
        String result = input.trim();
        // Simple strip tags (regex-based — not perfect but works for last 1% demo)
        result = result.replaceAll("<[^>]*>", "");
        return result;
    }
}
