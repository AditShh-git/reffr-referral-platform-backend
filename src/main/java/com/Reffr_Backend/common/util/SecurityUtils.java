package com.Reffr_Backend.common.util;

import com.Reffr_Backend.module.auth.security.JwtAuthenticationFilter.MinimalPrincipal;
import com.Reffr_Backend.module.auth.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Returns the UUID of the currently authenticated user.
     * Works with both {@link UserPrincipal} (OAuth2 flow) and
     * {@link MinimalPrincipal} (JWT filter flow).
     */
    public static UUID getCurrentUserId() {
        Object principal = getAuthPrincipal();
        if (principal instanceof UserPrincipal up)      return up.getId();
        if (principal instanceof MinimalPrincipal mp)   return mp.id();
        throw new IllegalStateException("Unknown principal type: " + principal.getClass());
    }

    /**
     * Returns the GitHub username of the currently authenticated user.
     */
    public static String getCurrentUserGithubUsername() {
        Object principal = getAuthPrincipal();
        if (principal instanceof UserPrincipal up)      return up.getGithubUsername();
        if (principal instanceof MinimalPrincipal mp)   return mp.githubUsername();
        throw new IllegalStateException("Unknown principal type: " + principal.getClass());
    }

    private static Object getAuthPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return auth.getPrincipal();
    }
}
