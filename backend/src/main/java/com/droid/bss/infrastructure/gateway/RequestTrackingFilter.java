package com.droid.bss.infrastructure.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request Tracking Filter
 * Tracks request rates per IP and enforces rate limiting
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestTrackingFilter implements GlobalFilter {

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger blockedRequests = new AtomicInteger(0);

    private static final int MAX_REQUESTS_PER_MINUTE = 1000;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIP = getClientIP(request);

        // Track total requests
        totalRequests.incrementAndGet();

        // Check rate limit
        if (!checkRateLimit(clientIP)) {
            blockedRequests.incrementAndGet();
            log.warn("Rate limit exceeded for IP: {} - Request: {} {}",
                clientIP, request.getMethod(), request.getURI());

            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory()
                    .wrap("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests\"}".getBytes())
            ));
        }

        return chain.filter(exchange);
    }

    private boolean checkRateLimit(String clientIP) {
        long now = System.currentTimeMillis();

        requestCounts.computeIfAbsent(clientIP, k -> new RequestCounter());

        RequestCounter counter = requestCounts.get(clientIP);

        // Clean old entries and check current window
        counter.cleanup(now, WINDOW_SIZE_MS);

        // Check if under limit
        boolean allowed = counter.getRequestCount() < MAX_REQUESTS_PER_MINUTE;

        if (allowed) {
            counter.increment();
        }

        return allowed;
    }

    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getBlockedRequests() {
        return blockedRequests.get();
    }

    public double getBlockRate() {
        int total = totalRequests.get();
        if (total == 0) return 0.0;
        return (blockedRequests.get() * 100.0) / total;
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long lastUpdateTime = System.currentTimeMillis();

        void increment() {
            count.incrementAndGet();
            lastUpdateTime = System.currentTimeMillis();
        }

        int getRequestCount() {
            return count.get();
        }

        void cleanup(long now, long windowSize) {
            if (now - lastUpdateTime > windowSize) {
                count.set(0);
                lastUpdateTime = now;
            }
        }
    }
}
