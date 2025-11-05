package com.droid.bss.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.vavr.control.Try;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Utility class for decorating methods with resilience patterns
 */
public class ResilienceDecorators {

    /**
     * Decorate a supplier with circuit breaker, retry, and time limiter
     */
    public static <T> Supplier<T> decorateWithResilience(
            Supplier<T> supplier,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter) {

        return () -> {
            Try.CheckedSupplier<T> decoratedSupplier = CircuitBreaker.decorateCheckedSupplier(
                    circuitBreaker,
                    Retry.decorateCheckedSupplier(retry, supplier)
            );

            return timeLimiter.executeSupplier(decoratedSupplier);
        };
    }

    /**
     * Decorate a runnable with circuit breaker, retry, and time limiter
     */
    public static Runnable decorateWithResilience(
            Runnable runnable,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter) {

        return () -> {
            Try.CheckedRunnable decoratedRunnable = CircuitBreaker.decorateCheckedRunnable(
                    circuitBreaker,
                    Retry.decorateCheckedRunnable(retry, runnable)
            );

            timeLimiter.executeRunnable(decoratedRunnable);
        };
    }

    /**
     * Execute with circuit breaker only
     */
    public static <T> T executeWithCircuitBreaker(
            Supplier<T> supplier,
            CircuitBreaker circuitBreaker) {

        return circuitBreaker.executeSupplier(supplier);
    }

    /**
     * Execute with retry only
     */
    public static <T> T executeWithRetry(
            Supplier<T> supplier,
            Retry retry) {

        return retry.executeSupplier(supplier);
    }

    /**
     * Execute with time limiter only
     */
    public static <T> T executeWithTimeLimiter(
            Supplier<T> supplier,
            TimeLimiter timeLimiter) {

        return timeLimiter.executeSupplier(supplier);
    }

    /**
     * Execute asynchronously with resilience
     */
    public static <T> CompletableFuture<T> executeAsyncWithResilience(
            Supplier<CompletableFuture<T>> supplier,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter,
            Executor executor) {

        return timeLimiter.executeCompletionStage(
                executor,
                () -> circuitBreaker.executeCompletionStage(supplier)
        ).toCompletableFuture();
    }

    /**
     * Check if circuit breaker is open
     */
    public static boolean isCircuitOpen(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Get circuit breaker state
     */
    public static String getCircuitState(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState().name();
    }

    /**
     * Get failure rate
     */
    public static float getFailureRate(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getMetrics().getFailureRate();
    }

    /**
     * Get call duration (P50, P95, P99)
     */
    public static CircuitBreaker.Metrics getMetrics(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getMetrics();
    }
}
