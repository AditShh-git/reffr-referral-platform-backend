package com.Reffr_Backend.common.config;

import com.Reffr_Backend.common.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final com.Reffr_Backend.common.security.IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply ONLY on specific endpoints based on user rules
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/chats/*/messages",
                        "/api/v1/referrals/**",
                        "/api/v1/auth/**",
                        "/api/v1/posts/**"
                );

        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/v1/posts", "/api/v1/referrals/**");
    }
}
