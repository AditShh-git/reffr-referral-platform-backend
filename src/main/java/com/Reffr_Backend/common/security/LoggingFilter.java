package com.Reffr_Backend.common.security;

import com.Reffr_Backend.common.util.SecurityUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class LoggingFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("requestId", requestId);
        
        try {
            UUID userId = SecurityUtils.getCurrentUserId();
            MDC.put("userId", userId.toString());
        } catch (Exception e) {
            MDC.put("userId", "anonymous");
        }

        MDC.put("endpoint", httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
