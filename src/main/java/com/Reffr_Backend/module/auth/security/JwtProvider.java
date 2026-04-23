package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.common.constants.SecurityConstants;
import com.Reffr_Backend.common.constants.TokenType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long      accessExpirationMs;
    private final long      refreshExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}")             String secret,
            @Value("${jwt.access-token-expiry-ms}")         long   accessExpiration,
            @Value("${jwt.refresh-token-expiry-ms}") long   refreshExpiration) {
        this.key                 = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(Base64.getEncoder().encodeToString(secret.getBytes())));
        this.accessExpirationMs  = accessExpiration;
        this.refreshExpirationMs = refreshExpiration;
    }

    // ── Token generation ──────────────────────────────────────────────

    public String generateAccessToken(UUID userId, String username, String deviceId) {
        return build(userId, username, deviceId, TokenType.ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(UUID userId, String username, String deviceId) {
        return build(userId, username, deviceId, TokenType.REFRESH, refreshExpirationMs);
    }

    private String build(UUID userId, String username, String deviceId,
                         TokenType type, long ttl) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + ttl);
        return Jwts.builder()
                .subject(userId.toString())
                .claim(SecurityConstants.CLAIM_USERNAME,   username)
                .claim(SecurityConstants.CLAIM_TOKEN_TYPE, type.name()) // enum → name
                .claim(SecurityConstants.CLAIM_DEVICE_ID,  deviceId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // ── Validation ────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e)   { log.error("JWT expired", e);           }
        catch (MalformedJwtException e)   { log.error("JWT malformed", e);          }
        catch (SecurityException e)       { log.error("JWT signature invalid", e);  }
        catch (IllegalArgumentException e){ log.error("JWT empty", e);              }
        return false;
    }

    // ── Claims extraction ─────────────────────────────────────────────

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).get(SecurityConstants.CLAIM_USERNAME, String.class);
    }

    /** Fix 4: Returns enum — callers use .isRefresh() / .isAccess(), no string compare */
    public TokenType getTokenType(String token) {
        String raw = parseClaims(token).get(SecurityConstants.CLAIM_TOKEN_TYPE, String.class);
        return TokenType.valueOf(raw);
    }

    public String getDeviceId(String token) {
        return parseClaims(token).get(SecurityConstants.CLAIM_DEVICE_ID, String.class);
    }

    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}