package com.droid.bss.infrastructure.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.Arrays;

/**
 * API Gateway Configuration
 * Configures routing, load balancing, and cross-cutting concerns
 * for the BSS system microservices
 */
@Configuration
public class ApiGatewayConfig {

    /**
     * Configure routes to microservices
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

            // Backend Service (GraphQL + REST + RSocket)
            .route("backend-service", r -> r
                .path("/api/v1/**", "/graphql", "/rsocket/**", "/actuator/**")
                .filters(f -> f
                    // Strip prefix
                    .stripPrefix(2)
                    // Circuit breaker
                    .circuitBreaker(config -> config
                        .setName("backend-circuit-breaker")
                        .setFallbackUri("forward:/fallback/backend"))
                    // Retry configuration
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setSeries(Arrays.asList(
                            org.springframework.http.HttpStatus.Series.SERVER_ERROR,
                            org.springframework.http.HttpStatus.Series.CLIENT_ERROR))
                        .setMethods("GET", "POST", "PUT", "DELETE")
                        .setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true))
                    // Rate limiting
                    .requestRateLimiter(config -> config
                        .setRateLimiter(gatewayRateLimiter())
                        .setKeyResolver(userKeyResolver()))
                    // Add headers
                    .addResponseHeader("X-Gateway-Version", "1.0.0")
                    .addResponseHeader("X-Response-Time", "${java.time.LocalDateTime.now()}")
                    // Rewrite path
                    .rewritePath("/api/v1/(?<segment>.*)", "/${segment}")
                    // Dedupe response header
                    .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                    // Secure headers
                    .modifyResponseBody(String.class, String.class, (exchange, body) -> {
                        exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
                        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
                        exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
                        exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                        return body;
                    }))
                .uri("lb://bss-backend"))

            // Health check endpoint (bypass circuit breaker)
            .route("health-check", r -> r
                .path("/health")
                .filters(f -> f
                    .addResponseHeader("X-Health-Check", "true")
                    .preserveHostHeader())
                .uri("lb://bss-backend/actuator/health"))

            // GraphQL Playground (development only)
            .route("graphql-playground", r -> r
                .path("/graphql-playground/**")
                .filters(f -> f
                    .rewritePath("/graphql-playground/(?<segment>.*)", "/graphql/$1")
                    .addRequestHeader("X-GraphQL-Playground", "true"))
                .uri("lb://bss-backend"))

            // RSocket Gateway (WebSocket transport)
            .route("rsocket-websocket", r -> r
                .path("/ws/rsocket")
                .filters(f -> f
                    .preserveHostHeader()
                    .addRequestHeader("X-RSocket-Transport", "websocket"))
                .uri("lb://bss-backend/rsocket"))

            // Metrics endpoint (for Prometheus)
            .route("metrics", r -> r
                .path("/metrics")
                .filters(f -> f
                    .addResponseHeader("X-Metrics-Endpoint", "true"))
                .uri("lb://bss-backend/actuator/prometheus"))

            // Static resources
            .route("static-resources", r -> r
                .path("/static/**")
                .filters(f -> f
                    .addResponseHeader("Cache-Control", "public, max-age=3600")
                    .rewritePath("/static/(?<segment>.*)", "/${segment}"))
                .uri("lb://bss-backend/static"))

            // Fallback routes
            .route("graphql-fallback", r -> r
                .path("/fallback/graphql")
                .uri("forward:/fallback/graphql"))

            .build();
    }

    /**
     * Custom Gateway Rate Limiter
     */
    @Bean
    public RedisRateLimiter gatewayRateLimiter() {
        // Default rate: 100 requests per second per key
        return new RedisRateLimiter(100, 200, 1000);
    }

    /**
     * User-based key resolver for rate limiting
     */
    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                .getFirst("X-User-ID");
            if (userId == null) {
                userId = exchange.getRequest().getRemoteAddress()
                    .getAddress().getHostAddress();
            }
            return Mono.just(userId);
        };
    }

    /**
     * CORS Configuration for the gateway
     */
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("HEAD");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
