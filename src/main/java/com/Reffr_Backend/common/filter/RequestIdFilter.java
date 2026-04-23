package com.Reffr_Backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs on EVERY request — before anything else.
 *
 * Puts these into SLF4J MDC (Mapped Diagnostic Context):
 *   requestId  — unique ID per request (trace all logs for one call)
 *   method     — GET / POST / etc.
 *   uri        — /api/v1/users/me
 *   userAgent  — browser / Postman / etc.
 *
 * MDC values are automatically printed in every log line
 * because logback.xml references them with %X{requestId} etc.
 */
@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID    = "requestId";
    private static final String MDC_METHOD        = "method";
    private static final String MDC_URI           = "uri";
    private static final String MDC_USER_AGENT    = "userAgent";

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {
        try {
            // Use caller-supplied ID if present (e.g. from frontend), else generate
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString().substring(0, 8); // short 8-char ID
            }

            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_METHOD,     request.getMethod());
            MDC.put(MDC_URI,        request.getRequestURI());
            MDC.put(MDC_USER_AGENT, request.getHeader("User-Agent") != null
                    ? request.getHeader("User-Agent").substring(0, Math.min(50,
                    request.getHeader("User-Agent").length()))
                    : "unknown");

            // Echo the request ID back so frontend can correlate
            response.setHeader(REQUEST_ID_HEADER, requestId);

            chain.doFilter(request, response);

        } finally {
            // ALWAYS clear MDC — threads are reused in Tomcat thread pool
            // Not clearing = previous request's data bleeds into next request
            MDC.clear();
        }
    }
}

