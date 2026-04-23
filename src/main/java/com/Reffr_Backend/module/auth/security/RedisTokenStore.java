package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.common.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fix 5: Multi-device session support.
 *
 * Key structure:
 *   session:{userId}:{deviceId}  → refreshToken   (one per device)
 *   denylist:{tokenHash}         → "revoked"      (short-lived, until token expiry)
 *
 * A user can have N concurrent sessions (phone, laptop, work machine).
 * Logging out on one device only revokes that device's token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void saveRefreshToken(UUID userId, String deviceId, String token) {
        String key = sessionKey(userId, deviceId);
        redisTemplate.opsForValue().set(key, token, REFRESH_TTL);
        log.debug("Saved refresh token for user={} device={}", userId, deviceId);
    }

    @Override
    public String getRefreshToken(UUID userId, String deviceId) {
        String value = redisTemplate.opsForValue().get(sessionKey(userId, deviceId));
        return value != null ? value : "";
    }

    @Override
    public void deleteRefreshToken(UUID userId, String deviceId) {
        redisTemplate.delete(sessionKey(userId, deviceId));
        log.debug("Deleted session for user={} device={}", userId, deviceId);
    }

    @Override
    public void deleteAllRefreshTokens(UUID userId) {
        // Scan all sessions for this user and delete them
        String pattern = SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Revoked {} sessions for user={}", keys.size(), userId);
        }
    }

    @Override
    public Set<String> getActiveDeviceIds(UUID userId) {
        String pattern = SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) return Set.of();

        // Extract deviceId from "session:{userId}:{deviceId}"
        String prefix = SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":";
        return keys.stream()
                .map(k -> k.substring(prefix.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public void denylistToken(String token, long ttlMillis) {
        if (ttlMillis <= 0) return;
        String key = SecurityConstants.TOKEN_DENYLIST_PREFIX + tokenHash(token);
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isDenylisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(SecurityConstants.TOKEN_DENYLIST_PREFIX + tokenHash(token)));
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String sessionKey(UUID userId, String deviceId) {
        return SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":" + deviceId;
    }

    /**
     * Hash the token before storing as key — avoids leaking full JWTs into Redis keyspace.
     * Uses last 16 chars which are sufficiently unique for denylist purposes.
     */
    private String tokenHash(String token) {
        int len = token.length();
        return len > 16 ? token.substring(len - 16) : token;
    }
}
