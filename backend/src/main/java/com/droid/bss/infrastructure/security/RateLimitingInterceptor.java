package com.droid.bss.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Interceptor for rate limiting
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingInterceptor.class);

    private final RateLimitingService rateLimitingService;

    public RateLimitingInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimiting rateLimiting = handlerMethod.getMethodAnnotation(RateLimiting.class);
        if (rateLimiting == null) {
            // Check class-level annotation
            rateLimiting = handlerMethod.getBeanType().getAnnotation(RateLimiting.class);
        }

        if (rateLimiting == null) {
            return true; // No rate limiting annotation, allow request
        }

        String method = request.getMethod();
        String path = request.getRequestURI();
        String key = rateLimitingService.getRateLimitKey(rateLimiting.keyPrefix());
        key += ":" + method + ":" + path;

        boolean allowed = rateLimitingService.isAllowed(key, rateLimiting.value(), rateLimiting.timeWindow());

        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
            return false;
        }

        return true;
    }
}
