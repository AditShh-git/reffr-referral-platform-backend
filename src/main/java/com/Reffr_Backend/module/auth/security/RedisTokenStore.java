package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.common.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTokenStore implements TokenStore {

    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void saveRefreshToken(UUID userId, String deviceId, String token) {
        redisTemplate.opsForValue().set(sessionKey(userId, deviceId), token, REFRESH_TTL);
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
        Set<String> keys = scanKeys(SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Revoked {} sessions for user={}", keys.size(), userId);
        }
    }

    @Override
    public Set<String> getActiveDeviceIds(UUID userId) {
        Set<String> keys = scanKeys(SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":*");
        if (keys.isEmpty()) {
            return Set.of();
        }

        String prefix = SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":";
        return keys.stream()
                .map(key -> key.substring(prefix.length()))
                .collect(Collectors.toSet());
    }

    @Override
    public void denylistToken(String token, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(
                SecurityConstants.TOKEN_DENYLIST_PREFIX + tokenHash(token),
                "revoked",
                Duration.ofMillis(ttlMillis)
        );
    }

    @Override
    public boolean isDenylisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(SecurityConstants.TOKEN_DENYLIST_PREFIX + tokenHash(token))
        );
    }

    private String sessionKey(UUID userId, String deviceId) {
        return SecurityConstants.REFRESH_TOKEN_PREFIX + userId + ":" + deviceId;
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(500).build())) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception ex) {
            log.warn("Redis SCAN failed for pattern={}: {}", pattern, ex.getMessage());
        }
        return keys;
    }

    private String tokenHash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
