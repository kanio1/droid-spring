package com.droid.bss.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuration for resilience patterns: Circuit Breaker, Retry, Time Limiter
 */
@Configuration
public class ResilienceConfig {

    private static final String CUSTOMER_SERVICE = "customerService";
    private static final String BILLING_SERVICE = "billingService";
    private static final String ASSET_SERVICE = "assetService";
    private static final String NOTIFICATION_SERVICE = "notificationService";

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(getCircuitBreakerConfig());
    }

    private CircuitBreakerConfig getCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // Sliding window size for recording calls
                .slidingWindowSize(10)
                // Minimum number of calls to evaluate failure rate
                .minimumNumberOfCalls(5)
                // Failure rate threshold (percentage)
                .failureRateThreshold(50.0f)
                // Wait duration in open state before transitioning to half-open
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Number of successful calls in half-open state to close circuit
                .permittedNumberOfCallsInHalfOpenState(3)
                // Slow call duration threshold
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // Slow call rate threshold (percentage)
                .slowCallRateThreshold(50.0f)
                // Automatic transition from open to half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Bean
    public CircuitBreaker customerServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(CUSTOMER_SERVICE, getCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker billingServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(20) // Larger window for billing
                .minimumNumberOfCalls(10)
                .failureRateThreshold(30.0f) // More strict for billing
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slowCallRateThreshold(40.0f)
                .build();
        return registry.circuitBreaker(BILLING_SERVICE, config);
    }

    @Bean
    public CircuitBreaker assetServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(ASSET_SERVICE, getCircuitBreakerConfig());
    }

    @Bean
    public CircuitBreaker notificationServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = getCircuitBreakerConfig();
        // Note: Custom config simplified for API compatibility
        return registry.circuitBreaker(NOTIFICATION_SERVICE, config);
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.<String>custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                // Note: exponentialBackoffMultiplier removed in newer versions
                .retryExceptions(
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .ignoreExceptions(
                        java.lang.IllegalArgumentException.class,
                        jakarta.validation.ValidationException.class
                )
                .build();
    }

    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(10, r -> {
            Thread t = new Thread(r, "resilience-scheduler");
            t.setDaemon(true);
            return t;
        });
    }
}
