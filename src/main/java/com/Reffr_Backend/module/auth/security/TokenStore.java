package com.Reffr_Backend.module.auth.security;

import java.util.Set;
import java.util.UUID;

/**
 * Abstracts token persistence — AuthService never touches Redis directly.
 * Swap implementation for tests or different backends without changing business logic.
 */
public interface TokenStore {

    /**
     * Persist a refresh token for a specific user session (device).
     * Multiple sessions per user are supported — each device gets its own slot.
     */
    void saveRefreshToken(UUID userId, String deviceId, String token);

    /**
     * Retrieve stored refresh token for a specific session.
     * Returns empty string if not found or expired.
     */
    String getRefreshToken(UUID userId, String deviceId);

    /**
     * Revoke a single session's refresh token (logout from one device).
     */
    void deleteRefreshToken(UUID userId, String deviceId);

    /**
     * Revoke all sessions for a user (logout from all devices).
     */
    void deleteAllRefreshTokens(UUID userId);

    /**
     * Returns all active device IDs for a user — for "active sessions" UI.
     */
    Set<String> getActiveDeviceIds(UUID userId);

    /**
     * Add an access token to the denylist until it naturally expires.
     */
    void denylistToken(String token, long ttlMillis);

    /**
     * Check if an access token has been explicitly revoked.
     */
    boolean isDenylisted(String token);
}
