package com.droid.bss.infrastructure.resilience;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stub implementation of RateLimitingInterceptor
 * Minimal implementation for testing purposes
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Stub implementation
        return true;
    }
}
