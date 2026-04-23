package com.Reffr_Backend.module.auth.service;

import com.Reffr_Backend.module.auth.dto.AuthDto;
import com.Reffr_Backend.module.auth.security.UserPrincipal;

import java.util.Set;
import java.util.UUID;

public interface AuthService {

    AuthDto.TokenResponse issueTokens(UserPrincipal principal, String deviceId);

    AuthDto.TokenResponse refreshToken(String refreshToken, String deviceId);

    void logout(String accessToken, UUID userId, String deviceId);

    void logoutAll(String accessToken, UUID userId);

    Set<String> getActiveDevices(UUID userId);

    boolean isTokenDenylisted(String token);
}