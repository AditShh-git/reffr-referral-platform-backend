package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.module.auth.dto.AuthDto;
import com.Reffr_Backend.module.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;


import java.io.IOException;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${reffr.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String deviceId = request.getHeader("X-Device-Id");
        if (!StringUtils.hasText(deviceId)) {
            deviceId = UUID.randomUUID().toString();
        }

        AuthDto.TokenResponse tokens = authService.issueTokens(principal, deviceId);

        //  secure redirect using fragment
        String redirectUrl = frontendUrl + "/auth/callback#"
                + "access_token=" + tokens.getAccessToken()
                + "&refresh_token=" + tokens.getRefreshToken()
                + "&device_id=" + deviceId;

        log.info("====================================");
        log.info("ACCESS TOKEN: {}", tokens.getAccessToken());
        log.info("REFRESH TOKEN: {}", tokens.getRefreshToken());
        log.info("====================================");

        log.info("OAuth2 success — redirecting user: {}", principal.getGithubUsername());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}