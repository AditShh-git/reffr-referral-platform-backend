package com.Reffr_Backend.module.auth.controller;


import com.Reffr_Backend.common.response.ApiResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import com.Reffr_Backend.module.auth.dto.AuthDto;
import com.Reffr_Backend.module.auth.service.AuthService;
import com.Reffr_Backend.module.user.dto.UserDto;
import com.Reffr_Backend.module.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Auth endpoints — GitHub OAuth2 + JWT")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserProfileService userProfileService;

    @Operation(summary = "Initiate GitHub OAuth2 login",
            description = "Redirect your browser to this URL to start GitHub OAuth2 flow. " +
                    "On success, you'll be redirected to frontend with access_token and refresh_token.")
    @GetMapping("/github")
    public ResponseEntity<ApiResponse<String>> initiateGithubLogin() {
        return ResponseEntity.ok(ApiResponse.success(
                "Redirect your browser to: /oauth2/authorization/github", null));
    }

    @Operation(summary = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refresh(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request,
            @RequestHeader("X-Device-Id") String deviceId) {

        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed",
                authService.refreshToken(request.getRefreshToken(), deviceId)
        ));
    }

    @Operation(summary = "Logout — invalidates current session")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @RequestHeader("X-Device-Id") String deviceId) {

        String accessToken = extractToken(request);

        authService.logout(
                accessToken,
                SecurityUtils.getCurrentUserId(),
                deviceId
        );

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @Operation(summary = "Get currently authenticated user")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getMyProfile()));
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return "";
    }
}
