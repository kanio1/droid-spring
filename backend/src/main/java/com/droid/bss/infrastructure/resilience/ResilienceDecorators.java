package com.droid.bss.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class for decorating methods with resilience patterns
 * Updated for Resilience4j 2.x API
 */
public class ResilienceDecorators {

    /**
     * Decorate a supplier with circuit breaker, retry, and time limiter
     */
    public static <T> Supplier<T> decorateWithResilience(
            Supplier<T> supplier,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter,
            ScheduledExecutorService scheduledExecutorService) {

        return () -> {
            // In Resilience4j 2.x, use the instance methods for decoration
            // Chain circuit breaker and retry
            Supplier<T> withCircuitBreaker = () -> circuitBreaker.executeSupplier(supplier);
            Supplier<T> withRetry = () -> retry.executeSupplier(withCircuitBreaker);

            // TimeLimiter 2.x uses executeCompletionStage with ScheduledExecutorService
            CompletableFuture<T> future = CompletableFuture.supplyAsync(withRetry);
            return timeLimiter.executeCompletionStage(scheduledExecutorService, () ->
                future.toCompletableFuture()
            ).toCompletableFuture().join();
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
            TimeLimiter timeLimiter,
            ScheduledExecutorService scheduledExecutorService) {

        return timeLimiter.executeCompletionStage(scheduledExecutorService, () ->
            CompletableFuture.supplyAsync(supplier)
        ).toCompletableFuture().join();
    }

    /**
     * Execute asynchronously with resilience
     */
    public static <T> CompletableFuture<T> executeAsyncWithResilience(
            Supplier<CompletableFuture<T>> supplier,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter,
            ScheduledExecutorService scheduledExecutorService) {

        // In Resilience4j 2.x, circuit breaker executeCompletionStage was refactored
        // Use direct execution with circuit breaker and then wrap with time limiter
        return timeLimiter.executeCompletionStage(scheduledExecutorService, () ->
            CompletableFuture.supplyAsync(() ->
                circuitBreaker.executeSupplier(() -> supplier.get().join())
            )
        ).toCompletableFuture();
    }

    /**
     * Check if circuit breaker is open
     */
    public static boolean isCircuitOpen(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState() == CircuitBreaker.State.OPEN;
    }

    /**
     * Check if circuit breaker is half-open
     */
    public static boolean isCircuitHalfOpen(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN;
    }

    /**
     * Get circuit breaker state name
     */
    public static String getCircuitBreakerState(CircuitBreaker circuitBreaker) {
        return circuitBreaker.getState().name();
    }

    /**
     * Get current metrics snapshot
     */
    public static String getCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        return String.format("State: %s, Success calls: %d, Failure calls: %d",
            circuitBreaker.getState().name(),
            circuitBreaker.getMetrics().getNumberOfSuccessfulCalls(),
            circuitBreaker.getMetrics().getNumberOfFailedCalls()
        );
    }
}
