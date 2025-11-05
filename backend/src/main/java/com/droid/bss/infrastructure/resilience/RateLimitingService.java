package com.droid.bss.infrastructure.resilience;

import com.droid.bss.infrastructure.metrics.BusinessMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting service using sliding window algorithm
 */
@Service
public class RateLimitingService {

    private final ConcurrentHashMap<String, SlidingWindow> userLimits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlidingWindow> ipLimits = new ConcurrentHashMap<>();
    private final BusinessMetrics businessMetrics;

    // Configuration
    private static final int USER_RATE_LIMIT = 100; // requests per window
    private static final int IP_RATE_LIMIT = 200; // requests per window
    private static final int WINDOW_SIZE_SECONDS = 60; // 1 minute window

    public RateLimitingService(BusinessMetrics businessMetrics) {
        this.businessMetrics = businessMetrics;
    }

    /**
     * Check if user has exceeded rate limit
     */
    public boolean isUserRateLimited(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        SlidingWindow window = userLimits.computeIfAbsent(userId, k -> new SlidingWindow());
        boolean limited = window.isRateLimited(USER_RATE_LIMIT, WINDOW_SIZE_SECONDS);

        if (limited) {
            businessMetrics.incrementCustomerStatusChanged(); // Reuse metric for rate limit
        }

        return limited;
    }

    /**
     * Check if IP address has exceeded rate limit
     */
    public boolean isIPRateLimited(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        SlidingWindow window = ipLimits.computeIfAbsent(ipAddress, k -> new SlidingWindow());
        boolean limited = window.isRateLimited(IP_RATE_LIMIT, WINDOW_SIZE_SECONDS);

        if (limited) {
            businessMetrics.incrementCustomerStatusChanged(); // Reuse metric for rate limit
        }

        return limited;
    }

    /**
     * Check rate limits for a request
     */
    public RateLimitResult checkRateLimit(HttpServletRequest request) {
        String userId = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : null;
        String clientIP = getClientIP(request);

        boolean userLimited = isUserRateLimited(userId);
        boolean ipLimited = isIPRateLimited(clientIP);

        return new RateLimitResult(userLimited, ipLimited);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    /**
     * Sliding window rate limiter
     */
    private static class SlidingWindow {
        private final ConcurrentHashMap<Long, AtomicInteger> window = new ConcurrentHashMap<>();
        private static final long WINDOW_SIZE_MS = 60_000; // 1 minute in milliseconds

        public boolean isRateLimited(int maxRequests, int windowSizeSeconds) {
            long currentTime = System.currentTimeMillis();
            long windowStart = currentTime - (WINDOW_SIZE_MS);

            // Remove old entries
            window.entrySet().removeIf(entry -> entry.getKey() < windowStart);

            // Count requests in current window
            int totalRequests = window.values().stream()
                    .mapToInt(AtomicInteger::get)
                    .sum();

            if (totalRequests >= maxRequests) {
                return true;
            }

            // Add current request
            window.computeIfAbsent(currentTime, k -> new AtomicInteger(0)).incrementAndGet();

            return false;
        }
    }

    /**
     * Result of rate limit check
     */
    public static class RateLimitResult {
        private final boolean userRateLimited;
        private final boolean ipRateLimited;

        public RateLimitResult(boolean userRateLimited, boolean ipRateLimited) {
            this.userRateLimited = userRateLimited;
            this.ipRateLimited = ipRateLimited;
        }

        public boolean isUserRateLimited() {
            return userRateLimited;
        }

        public boolean isIPRateLimited() {
            return ipRateLimited;
        }

        public boolean isRateLimited() {
            return userRateLimited || ipRateLimited;
        }
    }
}
