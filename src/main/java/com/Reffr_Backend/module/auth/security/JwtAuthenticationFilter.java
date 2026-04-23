package com.Reffr_Backend.module.auth.security;

import com.Reffr_Backend.common.constants.TokenType;
import com.Reffr_Backend.module.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request before Spring's auth filters.
 * Validates the Bearer token, checks the denylist, and populates the SecurityContext.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider  jwtProvider;
    private final AuthService  authService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (StringUtils.hasText(token)
                && jwtProvider.validateToken(token)
                && jwtProvider.getTokenType(token) == TokenType.ACCESS
                && !authService.isTokenDenylisted(token)) {

            try {
                UUID   userId   = jwtProvider.getUserIdFromToken(token);
                String username = jwtProvider.getUsernameFromToken(token);

                // Build a minimal principal — no DB call needed; claims are authoritative
                var authority    = new SimpleGrantedAuthority("ROLE_USER");
                var authToken    = new UsernamePasswordAuthenticationToken(
                        new MinimalPrincipal(userId, username),
                        null,
                        List.of(authority)
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

            } catch (Exception ex) {
                log.error("Could not set authentication from JWT: {}", ex.getMessage(), ex);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Lightweight principal used when only JWT claims are needed (no DB lookup).
     * Services that need a full {@link UserPrincipal} should load via UserService.findById().
     */
    public record MinimalPrincipal(UUID id, String githubUsername) {}
}
