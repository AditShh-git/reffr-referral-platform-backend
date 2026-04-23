package com.Reffr_Backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every HTTP request with:
 *   → [req-id] --> GET /api/v1/users/me
 *   ← [req-id] <-- 200 GET /api/v1/users/me  (45ms)
 *   ← [req-id] <-- 404 GET /api/v1/users/xyz (12ms)
 *
 * Skips actuator/swagger noise to keep console clean.
 */
@Slf4j
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        log.info("--> {} {}", request.getMethod(), request.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int  status   = response.getStatus();

            // Different log levels based on status code
            if (status >= 500) {
                log.error("<-- {} {} {} ({}ms)",
                        status, request.getMethod(), request.getRequestURI(), duration);
            } else if (status >= 400) {
                log.warn("<-- {} {} {} ({}ms)",
                        status, request.getMethod(), request.getRequestURI(), duration);
            } else {
                log.info("<-- {} {} {} ({}ms)",
                        status, request.getMethod(), request.getRequestURI(), duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Skip noise — actuator health checks, swagger UI, favicon
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/api-docs")
                || uri.equals("/favicon.ico");
    }
}
