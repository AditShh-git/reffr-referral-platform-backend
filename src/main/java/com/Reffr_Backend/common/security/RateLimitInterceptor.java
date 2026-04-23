package com.Reffr_Backend.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.Reffr_Backend.common.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String key, String uri, String method) {
        String planName = resolvePlan(uri, method);
        String cacheKey = key + ":" + planName;
        return cache.computeIfAbsent(cacheKey, k -> newBucket(planName));
    }

    private String resolvePlan(String uri, String method) {
        if (uri.contains("/posts") && "POST".equalsIgnoreCase(method)) return "post_create";
        if (uri.contains("/referrals") && "POST".equalsIgnoreCase(method)) return "referral_apply";
        if (uri.contains("/chats")) return "chat";
        if (uri.contains("/auth")) return "auth";
        return "default";
    }

    private Bucket newBucket(String planName) {
        Bandwidth limit;
        switch (planName) {
            case "chat" -> limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
            case "auth" -> limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
            case "referral_apply" -> limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofDays(1)));
            case "post_create" -> limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofHours(1)));
            default -> limit = Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1)));
        }
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                return SecurityUtils.getCurrentUserId().toString();
            } catch (Exception e) {
                // Fallback to IP below
            }
        }
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = getClientKey(request);
        Bucket bucket = resolveBucket(key, request.getRequestURI(), request.getMethod());

        // Consume 1 token per request
        if (bucket.tryConsume(1)) {
            return true;
        }

        // Rate limit exceeded
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write("Too Many Requests - Rate limit exceeded");
        return false;
    }
}
