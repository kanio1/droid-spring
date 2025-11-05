package com.droid.bss.infrastructure.resilience;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Interceptor for rate limiting
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    public RateLimitingInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/health")) {
            return true;
        }

        RateLimitingService.RateLimitResult result = rateLimitingService.checkRateLimit(request);

        if (result.isRateLimited()) {
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("X-Rate-Limit-Limited", "true");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"retry_after\": 60}");
            return false;
        }

        response.setHeader("X-Rate-Limit-Limited", "false");
        return true;
    }
}
