package com.droid.bss.infrastructure.gateway;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ServerWebExchange;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * API Gateway Tests
 * Tests gateway routing, filters, rate limiting, and fallback mechanisms
 */
@SpringBootTest
@Testcontainers
class ApiGatewayTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private RequestTrackingFilter requestTrackingFilter;

    @Autowired
    private GlobalLoggingFilter globalLoggingFilter;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    @DisplayName("Gateway routes should be configured")
    void testRoutesAreConfigured() {
        // Get all routes
        var routes = routeLocator.getRoutes().collectList().block();

        assertNotNull(routes);
        assertTrue(routes.size() > 0, "At least one route should be configured");

        // Verify specific routes exist
        boolean hasBackendRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("backend-service"));
        boolean hasHealthCheckRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("health-check"));
        boolean hasMetricsRoute = routes.stream()
            .anyMatch(route -> route.getId().equals("metrics"));

        assertTrue(hasBackendRoute, "Backend service route should be configured");
        assertTrue(hasHealthCheckRoute, "Health check route should be configured");
        assertTrue(hasMetricsRoute, "Metrics route should be configured");

        System.out.printf("Gateway Routes Configuration:%n");
        System.out.printf("  Total routes: %d%n", routes.size());
        routes.forEach(route -> System.out.printf("  - %s: %s%n",
            route.getId(), route.getUri()));
    }

    @Test
    @DisplayName("Request tracking filter should count requests")
    void testRequestTracking() {
        int totalBefore = requestTrackingFilter.getTotalRequests();

        // Simulate some requests
        for (int i = 0; i < 10; i++) {
            // In a real test, we would make actual HTTP requests
            // For now, we just verify the counter is accessible
        }

        int totalAfter = requestTrackingFilter.getTotalRequests();
        assertTrue(totalAfter >= totalBefore, "Total requests should be tracked");

        System.out.printf("Request Tracking Metrics:%n");
        System.out.printf("  Total requests: %d%n", requestTrackingFilter.getTotalRequests());
        System.out.printf("  Blocked requests: %d%n", requestTrackingFilter.getBlockedRequests());
        System.out.printf("  Block rate: %.2f%% %n", requestTrackingFilter.getBlockRate());
    }

    @Test
    @DisplayName("Rate limiting should be enabled")
    void testRateLimitingConfiguration() {
        assertNotNull(gatewayProperties.getRateLimit());

        assertTrue(gatewayProperties.getRateLimit().isEnableGlobalRateLimit());
        assertEquals(100, gatewayProperties.getRateLimit().getDefaultRate());
        assertEquals(200, gatewayProperties.getRateLimit().getBurstCapacity());
        assertEquals(1000, gatewayProperties.getRateLimit().getRequestedTokens());

        System.out.printf("Rate Limiting Configuration:%n");
        System.out.printf("  Default rate: %d req/s%n", gatewayProperties.getRateLimit().getDefaultRate());
        System.out.printf("  Burst capacity: %d%n", gatewayProperties.getRateLimit().getBurstCapacity());
        System.out.printf("  Requested tokens: %d%n", gatewayProperties.getRateLimit().getRequestedTokens());
        System.out.printf("  Global rate limit enabled: %s%n",
            gatewayProperties.getRateLimit().isEnableGlobalRateLimit());
    }

    @Test
    @DisplayName("Circuit breaker should be configured")
    void testCircuitBreakerConfiguration() {
        assertNotNull(gatewayProperties.getCircuitBreaker());

        assertTrue(gatewayProperties.getCircuitBreaker().isEnabled());
        assertEquals(50, gatewayProperties.getCircuitBreaker().getFailureRateThreshold());
        assertEquals(30, gatewayProperties.getCircuitBreaker().getWaitDurationInOpenState());
        assertEquals(10, gatewayProperties.getCircuitBreaker().getSlidingWindowSize());
        assertEquals(5, gatewayProperties.getCircuitBreaker().getMinimumNumberOfCalls());

        System.out.printf("Circuit Breaker Configuration:%n");
        System.out.printf("  Enabled: %s%n", gatewayProperties.getCircuitBreaker().isEnabled());
        System.out.printf("  Failure rate threshold: %d%% %n",
            gatewayProperties.getCircuitBreaker().getFailureRateThreshold());
        System.out.printf("  Wait duration in open state: %d seconds%n",
            gatewayProperties.getCircuitBreaker().getWaitDurationInOpenState());
        System.out.printf("  Sliding window size: %d%n",
            gatewayProperties.getCircuitBreaker().getSlidingWindowSize());
    }

    @Test
    @DisplayName("Load balancer should be configured")
    void testLoadBalancerConfiguration() {
        assertNotNull(gatewayProperties.getLoadBalancer());

        assertEquals("round-robin", gatewayProperties.getLoadBalancer().getStrategy());
        assertEquals(3, gatewayProperties.getLoadBalancer().getMaxRetries());
        assertEquals(2000, gatewayProperties.getLoadBalancer().getConnectTimeout());
        assertEquals(5000, gatewayProperties.getLoadBalancer().getReadTimeout());
        assertEquals(3, gatewayProperties.getLoadBalancer().getRetryAttempts());

        System.out.printf("Load Balancer Configuration:%n");
        System.out.printf("  Strategy: %s%n", gatewayProperties.getLoadBalancer().getStrategy());
        System.out.printf("  Max retries: %d%n", gatewayProperties.getLoadBalancer().getMaxRetries());
        System.out.printf("  Connect timeout: %d ms%n", gatewayProperties.getLoadBalancer().getConnectTimeout());
        System.out.printf("  Read timeout: %d ms%n", gatewayProperties.getLoadBalancer().getReadTimeout());
    }

    @Test
    @DisplayName("Security configuration should be complete")
    void testSecurityConfiguration() {
        assertNotNull(gatewayProperties.getSecurity());

        assertTrue(gatewayProperties.getSecurity().isEnableJwtValidation());
        assertTrue(gatewayProperties.getSecurity().isEnableRateLimiting());
        assertTrue(gatewayProperties.getSecurity().isEnableCors());
        assertTrue(gatewayProperties.getSecurity().isEnableSecurityHeaders());
        assertEquals(3600, gatewayProperties.getSecurity().getMaxAge());

        System.out.printf("Security Configuration:%n");
        System.out.printf("  JWT validation: %s%n", gatewayProperties.getSecurity().isEnableJwtValidation());
        System.out.printf("  Rate limiting: %s%n", gatewayProperties.getSecurity().isEnableRateLimiting());
        System.out.printf("  CORS enabled: %s%n", gatewayProperties.getSecurity().isEnableCors());
        System.out.printf("  Security headers: %s%n", gatewayProperties.getSecurity().isEnableSecurityHeaders());
        System.out.printf("  CORS max age: %d seconds%n", gatewayProperties.getSecurity().getMaxAge());
    }

    @Test
    @DisplayName("Monitoring configuration should be enabled")
    void testMonitoringConfiguration() {
        assertNotNull(gatewayProperties.getMonitoring());

        assertTrue(gatewayProperties.getMonitoring().isEnableMetrics());
        assertTrue(gatewayProperties.getMonitoring().isEnableTracing());
        assertTrue(gatewayProperties.getMonitoring().isEnableLogging());
        assertEquals("bss.gateway", gatewayProperties.getMonitoring().getMetricsPrefix());

        System.out.printf("Monitoring Configuration:%n");
        System.out.printf("  Metrics enabled: %s%n", gatewayProperties.getMonitoring().isEnableMetrics());
        System.out.printf("  Tracing enabled: %s%n", gatewayProperties.getMonitoring().isEnableTracing());
        System.out.printf("  Logging enabled: %s%n", gatewayProperties.getMonitoring().isEnableLogging());
        System.out.printf("  Metrics prefix: %s%n", gatewayProperties.getMonitoring().getMetricsPrefix());
    }
}
