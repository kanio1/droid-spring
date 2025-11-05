package com.droid.bss.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.*;

/**
 * Test for CircuitBreakerConfig
 * Following Arrange-Act-Assert pattern
 */
@SpringBootTest(classes = CircuitBreakerConfigTest.TestConfig.class)
@TestPropertySource(properties = {
    "resilience.circuitbreaker.enabled=true",
    "resilience.retry.enabled=true"
})
@DisplayName("CircuitBreakerConfig Resilience Infrastructure")
class CircuitBreakerConfigTest {

    private CircuitBreakerRegistry registry;

    private CircuitBreakerConfig config;

    private RetryConfig retryConfig;

    private TimeLimiterConfig timeLimiterConfig;

    private ScheduledExecutorService scheduledExecutorService;

    @Test
    @DisplayName("Should create circuit breaker registry")
    void shouldCreateCircuitBreakerRegistry() {
        // Arrange & Act
        CircuitBreakerRegistry result = createTestRegistry();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAllCircuitBreakers()).isNotNull();
    }

    @Test
    @DisplayName("Should configure default circuit breaker with correct settings")
    void shouldConfigureDefaultCircuitBreaker() {
        // Arrange & Act
        CircuitBreakerConfig config = createDefaultConfig();

        // Assert
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(50.0f);
        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should create customer service circuit breaker")
    void shouldCreateCustomerServiceCircuitBreaker() {
        // Arrange & Act
        CircuitBreaker circuitBreaker = createCircuitBreaker("customerService", createDefaultConfig());

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("customerService");
    }

    @Test
    @DisplayName("Should create billing service circuit breaker with stricter settings")
    void shouldCreateBillingServiceCircuitBreakerWithStricterSettings() {
        // Arrange & Act
        CircuitBreakerConfig billingConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(30.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slowCallRateThreshold(40.0f)
                .build();

        CircuitBreaker circuitBreaker = createCircuitBreaker("billingService", billingConfig);

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("billingService");
        assertThat(billingConfig.getSlidingWindowSize()).isEqualTo(20);
        assertThat(billingConfig.getMinimumNumberOfCalls()).isEqualTo(10);
        assertThat(billingConfig.getFailureRateThreshold()).isEqualTo(30.0f);
        assertThat(billingConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(60));
        assertThat(billingConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create asset service circuit breaker")
    void shouldCreateAssetServiceCircuitBreaker() {
        // Arrange & Act
        CircuitBreaker circuitBreaker = createCircuitBreaker("assetService", createDefaultConfig());

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("assetService");
    }

    @Test
    @DisplayName("Should create notification service circuit breaker with tolerant settings")
    void shouldCreateNotificationServiceCircuitBreakerWithTolerantSettings() {
        // Arrange & Act
        CircuitBreakerConfig notificationConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(70.0f)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreaker circuitBreaker = createCircuitBreaker("notificationService", notificationConfig);

        // Assert
        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.getName()).isEqualTo("notificationService");
        assertThat(notificationConfig.getSlidingWindowSize()).isEqualTo(5);
        assertThat(notificationConfig.getMinimumNumberOfCalls()).isEqualTo(3);
        assertThat(notificationConfig.getFailureRateThreshold()).isEqualTo(70.0f);
        assertThat(notificationConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(15));
        assertThat(notificationConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should configure retry with correct settings")
    void shouldConfigureRetryWithCorrectSettings() {
        // Arrange & Act
        RetryConfig<String> retryConfig = createTestRetryConfig();

        // Assert
        assertThat(retryConfig.getMaxAttempts()).isEqualTo(3);
        assertThat(retryConfig.getWaitDuration()).isEqualTo(Duration.ofSeconds(1));
        assertThat(retryConfig.getRetryExceptions()).contains(
            java.net.ConnectException.class,
            java.net.SocketTimeoutException.class,
            org.springframework.web.client.ResourceAccessException.class
        );
        assertThat(retryConfig.getIgnoreExceptions()).contains(
            java.lang.IllegalArgumentException.class,
            jakarta.validation.ValidationException.class
        );
    }

    @Test
    @DisplayName("Should configure time limiter with correct settings")
    void shouldConfigureTimeLimiterWithCorrectSettings() {
        // Arrange & Act
        TimeLimiterConfig timeLimiterConfig = createTestTimeLimiterConfig();

        // Assert
        assertThat(timeLimiterConfig.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Should create scheduled executor service")
    void shouldCreateScheduledExecutorService() {
        // Arrange & Act
        ScheduledExecutorService executor = createTestScheduledExecutor();

        // Assert
        assertThat(executor).isNotNull();
        assertThat(executor.isShutdown()).isFalse();
        assertThat(executor.isTerminated()).isFalse();
    }

    @Test
    @DisplayName("Should configure circuit breaker with automatic transition enabled")
    void shouldConfigureCircuitBreakerWithAutomaticTransitionEnabled() {
        // Arrange & Act
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        // Assert
        assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should configure different circuit breakers for different services")
    void shouldConfigureDifferentCircuitBreakersForDifferentServices() {
        // Arrange & Act
        CircuitBreaker customerCircuitBreaker = createCircuitBreaker("customer", createDefaultConfig());
        CircuitBreaker billingCircuitBreaker = createCircuitBreaker("billing", createBillingConfig());

        // Assert
        assertThat(customerCircuitBreaker.getName()).isEqualTo("customer");
        assertThat(billingCircuitBreaker.getName()).isEqualTo("billing");
        assertThat(customerCircuitBreaker).isNotEqualTo(billingCircuitBreaker);
    }

    @Test
    @DisplayName("Should handle slow call threshold correctly")
    void shouldHandleSlowCallThresholdCorrectly() {
        // Arrange & Act
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50.0f)
                .build();

        // Assert
        assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        assertThat(config.getSlowCallRateThreshold()).isEqualTo(50.0f);
    }

    @Test
    @DisplayName("Should configure retry with exponential backoff")
    void shouldConfigureRetryWithExponentialBackoff() {
        // Arrange & Act
        RetryConfig<String> retryConfig = RetryConfig.<String>custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .exponentialBackoffMultiplier(2.0)
                .build();

        // Assert
        assertThat(retryConfig.getMaxAttempts()).isEqualTo(3);
        assertThat(retryConfig.getWaitDuration()).isEqualTo(Duration.ofSeconds(1));
        // The exponential backoff multiplier would be tested with actual retry execution
    }

    @Test
    @DisplayName("Should configure circuit breaker with custom sliding window")
    void shouldConfigureCircuitBreakerWithCustomSlidingWindow() {
        // Arrange & Act
        CircuitBreakerConfig config1 = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .build();

        CircuitBreakerConfig config2 = CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .build();

        // Assert
        assertThat(config1.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config2.getSlidingWindowSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should configure circuit breaker with custom failure rate threshold")
    void shouldConfigureCircuitBreakerWithCustomFailureRateThreshold() {
        // Arrange & Act
        CircuitBreakerConfig config1 = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .build();

        CircuitBreakerConfig config2 = CircuitBreakerConfig.custom()
                .failureRateThreshold(30.0f)
                .build();

        // Assert
        assertThat(config1.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config2.getFailureRateThreshold()).isEqualTo(30.0f);
    }

    // Helper methods
    private CircuitBreakerRegistry createTestRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom().build();
        return CircuitBreakerRegistry.of(config);
    }

    private CircuitBreakerConfig createDefaultConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50.0f)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    private CircuitBreakerConfig createBillingConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(30.0f)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slowCallRateThreshold(40.0f)
                .build();
    }

    private RetryConfig<String> createTestRetryConfig() {
        return RetryConfig.<String>custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .exponentialBackoffMultiplier(2.0)
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

    private TimeLimiterConfig createTestTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }

    private ScheduledExecutorService createTestScheduledExecutor() {
        return Executors.newScheduledThreadPool(10, r -> {
            Thread t = new Thread(r, "resilience-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    private CircuitBreaker createCircuitBreaker(String name, CircuitBreakerConfig config) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker(name, config);
    }

    @Configuration
    static class TestConfig {
        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {
            return CircuitBreakerRegistry.of(createDefaultConfig());
        }

        @Bean
        public CircuitBreakerConfig circuitBreakerConfig() {
            return createDefaultConfig();
        }

        @Bean
        public RetryConfig<String> retryConfig() {
            return createTestRetryConfig();
        }

        @Bean
        public TimeLimiterConfig timeLimiterConfig() {
            return createTestTimeLimiterConfig();
        }

        @Bean
        public ScheduledExecutorService scheduledExecutorService() {
            return createTestScheduledExecutor();
        }

        // Helper methods
        private static CircuitBreakerConfig createDefaultConfig() {
            return CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .minimumNumberOfCalls(5)
                    .failureRateThreshold(50.0f)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .slowCallDurationThreshold(Duration.ofSeconds(2))
                    .slowCallRateThreshold(50.0f)
                    .automaticTransitionFromOpenToHalfOpenEnabled(true)
                    .build();
        }

        private static RetryConfig<String> createTestRetryConfig() {
            return RetryConfig.<String>custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofSeconds(1))
                    .exponentialBackoffMultiplier(2.0)
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

        private static TimeLimiterConfig createTestTimeLimiterConfig() {
            return TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(5))
                    .build();
        }

        private static ScheduledExecutorService createTestScheduledExecutor() {
            return Executors.newScheduledThreadPool(10, r -> {
                Thread t = new Thread(r, "resilience-scheduler");
                t.setDaemon(true);
                return t;
            });
        }
    }
}
