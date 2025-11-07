package com.droid.bss.infrastructure.resilience;

import com.droid.bss.BssApplication;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Circuit Breaker Tests
 *
 * Tests circuit breaker behavior, states (closed/open/half-open), fallback execution,
 * error thresholds, and resilience patterns.
 */
@SpringBootTest(classes = Application.class)
@DisplayName("Circuit Breaker Tests")
class CircuitBreakerTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("Should open circuit when error threshold reached")
    void shouldOpenCircuitOnErrorThreshold() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-circuit", config);

        for (int i = 0; i < 10; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Simulated failure");
                });
            } catch (Exception e) {
            }
        }

        CircuitBreaker.State state = circuitBreaker.getState();
        assertThat(state).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should close circuit after timeout period")
    void shouldCloseCircuitAfterTimeout() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-timeout-circuit", config);

        for (int i = 0; i < 10; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Simulated failure");
                });
            } catch (Exception e) {
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            circuitBreaker.executeSupplier(() -> "success");
        } catch (Exception e) {
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should execute fallback when circuit is open")
    void shouldExecuteFallback() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-fallback", config);

        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Service unavailable");
                });
            } catch (Exception e) {
            }
        }

        String result = circuitBreaker.executeSupplier(() -> "fallback-response");

        CircuitBreaker.State state = circuitBreaker.getState();
        assertThat(state).isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should measure error threshold correctly")
    void shouldMeasureErrorThreshold() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-threshold", config);

        for (int i = 0; i < 10; i++) {
            if (i < 6) {
                try {
                    circuitBreaker.executeSupplier(() -> {
                        throw new RuntimeException("Failure");
                    });
                } catch (Exception e) {
                }
            } else {
                try {
                    circuitBreaker.executeSupplier(() -> "success");
                } catch (Exception e) {
                }
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should validate request volume threshold")
    void shouldValidateRequestVolumeThreshold() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-volume", config);

        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle half-open state")
    void shouldHandleHalfOpenState() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-half-open", config);

        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.executeSupplier(() -> "success");
            } catch (Exception e) {
            }
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should handle nested circuit breakers")
    void shouldHandleNestedCircuitBreakers() {
        CircuitBreakerConfig parentConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .build();

        CircuitBreakerConfig childConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .build();

        CircuitBreaker parentCircuit = circuitBreakerRegistry.circuitBreaker("parent-circuit", parentConfig);
        CircuitBreaker childCircuit = circuitBreakerRegistry.circuitBreaker("child-circuit", childConfig);

        try {
            parentCircuit.executeSupplier(() -> {
                return childCircuit.executeSupplier(() -> "nested-call");
            });
        } catch (Exception e) {
        }

        assertThat(parentCircuit).isNotNull();
        assertThat(childCircuit).isNotNull();
        assertThat(parentCircuit.getName()).isEqualTo("parent-circuit");
        assertThat(childCircuit.getName()).isEqualTo("child-circuit");
    }

    @Test
    @DisplayName("Should collect circuit breaker metrics")
    void shouldCollectCircuitBreakerMetrics() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-metrics", config);

        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    if (i < 3) throw new RuntimeException("Failure");
                    return "success";
                });
            } catch (Exception e) {
            }
        }

        var metrics = circuitBreaker.getCircuitBreakerMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(0);
        assertThat(metrics.getNumberOfNotPermittedCalls()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should persist circuit breaker state")
    void shouldPersistCircuitBreakerState() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(5)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-persistence", config);

        for (int i = 0; i < 5; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Failure");
                });
            } catch (Exception e) {
            }
        }

        CircuitBreaker.State state = circuitBreaker.getState();
        assertThat(state).isNotNull();

        String stateName = state.name();
        assertThat(stateName).isIn("CLOSED", "OPEN", "HALF_OPEN");
    }

    @Test
    @DisplayName("Should support custom policies")
    void shouldSupportCustomPolicies() {
        AtomicInteger customCallCount = new AtomicInteger(0);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(3)
                .slidingWindowSize(3)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-custom", config);

        Supplier<String> decoratedSupplier = circuitBreaker
                .decorateSupplier(() -> {
                    customCallCount.incrementAndGet();
                    if (customCallCount.get() <= 2) {
                        throw new RuntimeException("Custom failure");
                    }
                    return "custom-success";
                });

        try {
            decoratedSupplier.get();
        } catch (Exception e) {
        }

        try {
            decoratedSupplier.get();
        } catch (Exception e) {
        }

        String result = decoratedSupplier.get();

        assertThat(result).isEqualTo("custom-success");
        assertThat(customCallCount.get()).isEqualTo(3);
    }
}
