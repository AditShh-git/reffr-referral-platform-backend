package com.Reffr_Backend.common.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyRecordRepository repository;

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

        // Check if this explicit request has already been processed successfully
        Optional<IdempotencyRecord> recordOpt = repository.findById(idempotencyKey);
        if (recordOpt.isPresent()) {
            log.info("Idempotency hit! Returning cached response for key: {}", idempotencyKey);
            IdempotencyRecord record = recordOpt.get();
            response.setStatus(record.getStatusCode());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponseBody());
            return;
        }

        // Cache the response to be saved later
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, responseWrapper);

        int status = responseWrapper.getStatus();
        String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

        // Save idempotency for successful actions (2xx) and client errors (4xx) handling. Ignore 5xx server errors for retry
        if (status >= 200 && status < 500) {
            IdempotencyRecord newRecord = new IdempotencyRecord(idempotencyKey, status, body, null);
            try {
                repository.save(newRecord);
                log.debug("Saved idempotency record for key: {}", idempotencyKey);
            } catch (Exception e) {
                // Ignore constraint violations under high concurrency
                log.warn("Failed to save idempotency record: {}", e.getMessage());
            }
        }

        responseWrapper.copyBodyToResponse();
    }
}
