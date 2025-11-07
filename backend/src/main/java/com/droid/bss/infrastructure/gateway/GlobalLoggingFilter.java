package com.droid.bss.infrastructure.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Global Logging Filter
 * Logs all incoming requests and responses with timing information
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalLoggingFilter implements GlobalFilter {

    private static final String START_TIME_KEY = "requestStartTime";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        exchange.getAttributes().put(REQUEST_ID_KEY, requestId);

        // Record start time
        LocalDateTime startTime = LocalDateTime.now();
        exchange.getAttributes().put(START_TIME_KEY, startTime);

        // Add request ID to headers
        Consumer<ServerHttpRequest.Builder> requestIdHeader =
            builder -> builder.header("X-Request-ID", requestId);

        // Log incoming request
        log.info("Incoming Request: {} {} - RequestID: {} - IP: {} - UserAgent: {}",
            request.getMethod(),
            request.getURI(),
            requestId,
            getClientIP(request),
            request.getHeaders().getFirst("User-Agent")
        );

        // Add request ID to response
        response.before(headers -> headers.set("X-Request-ID", requestId));
        response.after(headers -> {
            headers.set("X-Response-Time", LocalDateTime.now().toString());
        });

        return chain.filter(exchange.mutate().request(request.mutate().headers(requestIdHeader).build()).build())
            .doOnSuccess(aVoid -> {
                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime requestStartTime = exchange.getAttribute(START_TIME_KEY);

                if (requestStartTime != null) {
                    long durationMs = Duration.between(requestStartTime, endTime).toMillis();
                    int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;

                    String logMessage = String.format(
                        "Outgoing Response: %s %s - Status: %d - Duration: %d ms - RequestID: %s",
                        request.getMethod(),
                        request.getURI(),
                        statusCode,
                        durationMs,
                        requestId
                    );

                    // Log based on status code
                    if (statusCode >= 500) {
                        log.error(logMessage);
                    } else if (statusCode >= 400) {
                        log.warn(logMessage);
                    } else {
                        log.info(logMessage);
                    }

                    // Add duration header
                    response.getHeaders().add("X-Duration-Ms", String.valueOf(durationMs));

                    // Log slow requests
                    if (durationMs > 5000) {
                        log.warn("Slow Request Detected: {} {} took {} ms - RequestID: {}",
                            request.getMethod(),
                            request.getURI(),
                            durationMs,
                            requestId
                        );
                    }
                }
            })
            .doOnError(error -> {
                log.error("Request Failed: {} {} - Error: {} - RequestID: {}",
                    request.getMethod(),
                    request.getURI(),
                    error.getMessage(),
                    requestId,
                    error
                );

                // Add error information to response headers
                response.getHeaders().add("X-Error", error.getClass().getSimpleName());
                response.getHeaders().add("X-Error-Message", error.getMessage());
            });
    }

    private String getClientIP(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}
