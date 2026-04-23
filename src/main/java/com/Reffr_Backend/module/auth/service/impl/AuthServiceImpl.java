package com.Reffr_Backend.module.auth.service.impl;

import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.exception.NotFoundException;
import com.Reffr_Backend.common.exception.UnauthorizedException;
import com.Reffr_Backend.module.auth.dto.AuthDto;
import com.Reffr_Backend.module.auth.security.JwtProvider;
import com.Reffr_Backend.module.auth.security.TokenStore;
import com.Reffr_Backend.module.auth.security.UserPrincipal;
import com.Reffr_Backend.module.auth.service.AuthService;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.entity.User;
import com.Reffr_Backend.module.user.repository.UserRepository;

import com.Reffr_Backend.module.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtProvider        jwtProvider;
    private final TokenStore tokenStore;       // Fix 2: interface
    private final UserRepository     userRepository;
    private final UserProfileService userProfileService;

    // ── Issue tokens after GitHub OAuth2 ─────────────────────────────

    @Transactional
    public AuthDto.TokenResponse issueTokens(UserPrincipal principal, String deviceId) {
        UUID   userId   = principal.getId();
        String username = principal.getGithubUsername();

        String accessToken  = jwtProvider.generateAccessToken(userId, username, deviceId);
        String refreshToken = jwtProvider.generateRefreshToken(userId, username, deviceId);

        // Fix 5: store per-device — doesn't overwrite other sessions
        tokenStore.saveRefreshToken(userId, deviceId, refreshToken);

        userProfileService.updateLastSeen(userId);

        User user = userProfileService.findById(userId);
        log.info("Tokens issued — user={} device={}", username, deviceId);

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .deviceId(deviceId)
                .user(UserDto.UserSummary.from(user))
                .build();
    }

    // ── Refresh access token ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public AuthDto.TokenResponse refreshToken(String refreshToken, String deviceId) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException(ErrorCodes.TOKEN_INVALID, "Invalid or expired refresh token");
        }

        // Fix 4: enum comparison — no magic strings
        if (!jwtProvider.getTokenType(refreshToken).isRefresh()) {
            throw new UnauthorizedException(ErrorCodes.TOKEN_INVALID, "Token is not a refresh token");
        }

        UUID   userId   = jwtProvider.getUserIdFromToken(refreshToken);
        String username = jwtProvider.getUsernameFromToken(refreshToken);

        // Fix 5: validate against the specific device's stored token
        String stored = tokenStore.getRefreshToken(userId, deviceId);
        if (!refreshToken.equals(stored)) {
            throw new UnauthorizedException(ErrorCodes.TOKEN_EXPIRED, "Refresh token invalid or session expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "User not found"));
        if (!user.isActive()) {
            throw new UnauthorizedException(ErrorCodes.ACCESS_DENIED, "Account is deactivated");
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, username, deviceId);
        log.debug("Access token refreshed — user={} device={}", username, deviceId);

        return AuthDto.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .deviceId(deviceId)
                .user(UserDto.UserSummary.from(user))
                .build();
    }

    // ── Logout single device ──────────────────────────────────────────

    public void logout(String accessToken, UUID userId, String deviceId) {
        long expiresAt = jwtProvider.getExpirationFromToken(accessToken).getTime();
        long ttlMs     = expiresAt - System.currentTimeMillis();
        tokenStore.denylistToken(accessToken, ttlMs);
        tokenStore.deleteRefreshToken(userId, deviceId);
        log.info("Logged out — user={} device={}", userId, deviceId);
    }

    // ── Logout all devices ────────────────────────────────────────────

    public void logoutAll(String accessToken, UUID userId) {
        long expiresAt = jwtProvider.getExpirationFromToken(accessToken).getTime();
        long ttlMs     = expiresAt - System.currentTimeMillis();
        tokenStore.denylistToken(accessToken, ttlMs);
        tokenStore.deleteAllRefreshTokens(userId);
        log.info("Logged out all sessions — user={}", userId);
    }

    // ── Active sessions (for user's "manage devices" UI) ─────────────

    public Set<String> getActiveDevices(UUID userId) {
        return tokenStore.getActiveDeviceIds(userId);
    }

    public boolean isTokenDenylisted(String token) {
        return tokenStore.isDenylisted(token);
    }
}