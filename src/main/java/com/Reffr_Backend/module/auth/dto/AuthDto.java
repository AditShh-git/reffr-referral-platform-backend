package com.Reffr_Backend.module.auth.dto;

import com.Reffr_Backend.module.user.dto.UserDto;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

public final class AuthDto {

    private AuthDto() {}

    @Getter @Setter
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Getter @Builder
    public static class TokenResponse {
        private String              accessToken;
        private String              refreshToken;
        private String              tokenType;
        private Long                expiresIn;     // seconds
        private String              deviceId;
        private UserDto.UserSummary user;
    }
}
