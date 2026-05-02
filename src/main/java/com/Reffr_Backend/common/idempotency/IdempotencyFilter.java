package com.Reffr_Backend.common.idempotency;

import com.Reffr_Backend.module.auth.security.JwtAuthenticationFilter;
import com.Reffr_Backend.module.auth.security.JwtProvider;
import com.Reffr_Backend.module.auth.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final UUID ANONYMOUS_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final IdempotencyRecordRepository repository;
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        if (method.equals("GET") || method.equals("DELETE") || method.equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String scopedKey = buildScopedKey(request, idempotencyKey);
        Optional<IdempotencyRecord> recordOpt = repository.findById(scopedKey);
        if (recordOpt.isPresent()) {
            log.info("Idempotency hit for key={}", scopedKey);
            IdempotencyRecord record = recordOpt.get();
            response.setStatus(record.getStatusCode());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponseBody());
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        int status = responseWrapper.getStatus();
        String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

        if (status >= 200 && status < 500) {
            try {
                repository.save(new IdempotencyRecord(scopedKey, status, body, null));
                log.debug("Saved idempotency record for key={}", scopedKey);
            } catch (Exception ex) {
                log.warn("Failed to save idempotency record for key={}: {}", scopedKey, ex.getMessage());
            }
        }

        responseWrapper.copyBodyToResponse();
    }

    private String buildScopedKey(HttpServletRequest request, String rawKey) {
        return resolveCurrentUserId()
                + ":" + request.getMethod()
                + ":" + request.getRequestURI()
                + ":" + rawKey;
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getPrincipal() != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getId();
            }
            if (principal instanceof JwtAuthenticationFilter.MinimalPrincipal minimalPrincipal) {
                return minimalPrincipal.id();
            }
        }

        String authorization = requestToken();
        if (authorization != null && jwtProvider.validateToken(authorization)) {
            return jwtProvider.getUserIdFromToken(authorization);
        }

        return ANONYMOUS_USER_ID;
    }

    private String requestToken() {
        var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
            String header = servletAttributes.getRequest().getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return null;
    }
}
