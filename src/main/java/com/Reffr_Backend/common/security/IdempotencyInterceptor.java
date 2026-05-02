package com.Reffr_Backend.common.security;

import com.Reffr_Backend.common.exception.ConflictException;
import com.Reffr_Backend.common.exception.ErrorCodes;
import com.Reffr_Backend.common.security.annotation.Idempotent;
import com.Reffr_Backend.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final String IDEMPOTENCY_PREFIX = "idempotency:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (annotation == null) {
            return true;
        }

        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            return true; // Or throw error if mandatory
        }

        UUID userId;
        try {
            userId = SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            userId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // Anonymous
        }

        String cacheKey = IDEMPOTENCY_PREFIX
                + userId + ":"
                + request.getMethod() + ":"
                + request.getRequestURI() + ":"
                + key;
        
        Boolean success = redisTemplate.opsForValue().setIfAbsent(cacheKey, "PROCESSING", 
                annotation.ttlInSeconds(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            throw new ConflictException(ErrorCodes.DUPLICATE_REQUEST, 
                    "This request is already being processed or was recently completed.");
        }

        return true;
    }
}
