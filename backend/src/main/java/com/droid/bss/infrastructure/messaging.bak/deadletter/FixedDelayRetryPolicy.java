package com.droid.bss.infrastructure.messaging.deadletter;

import java.util.concurrent.TimeUnit;

/**
 * Fixed delay retry policy.
 *
 * Uses a fixed delay between retry attempts.
 *
 * @since 1.0
 */
public class FixedDelayRetryPolicy implements RetryPolicy {

    private final int maxRetries;
    private final long delayMs;
    private final boolean exponentialBackoff;

    /**
     * Creates a new FixedDelayRetryPolicy.
     *
     * @param maxRetries the maximum number of retries
     * @param delayMs the fixed delay in milliseconds
     * @param exponentialBackoff whether to use exponential backoff
     */
    public FixedDelayRetryPolicy(int maxRetries, long delayMs, boolean exponentialBackoff) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        if (delayMs <= 0) {
            throw new IllegalArgumentException("Delay must be positive");
        }
        this.maxRetries = maxRetries;
        this.delayMs = delayMs;
        this.exponentialBackoff = exponentialBackoff;
    }

    /**
     * Creates a new FixedDelayRetryPolicy with fixed delay.
     *
     * @param maxRetries the maximum number of retries
     * @param delay the delay
     * @param unit the time unit
     * @return the policy
     */
    public static RetryPolicy withFixedDelay(int maxRetries, long delay, TimeUnit unit) {
        return new FixedDelayRetryPolicy(maxRetries, unit.toMillis(delay), false);
    }

    /**
     * Creates a new FixedDelayRetryPolicy with exponential backoff.
     *
     * @param maxRetries the maximum number of retries
     * @param initialDelay the initial delay
     * @param unit the time unit
     * @return the policy
     */
    public static RetryPolicy withExponentialBackoff(int maxRetries, long initialDelay, TimeUnit unit) {
        return new FixedDelayRetryPolicy(maxRetries, unit.toMillis(initialDelay), true);
    }

    @Override
    public boolean shouldRetry(DLQEntry entry) {
        if (entry == null) {
            return false;
        }

        return entry.getRetryCount() < maxRetries;
    }

    @Override
    public long getRetryDelay(DLQEntry entry) {
        if (entry == null) {
            return delayMs;
        }

        if (exponentialBackoff) {
            // Exponential backoff: delay = initialDelay * 2^retryCount
            long exponentialDelay = delayMs * (1L << entry.getRetryCount());
            return Math.min(exponentialDelay, TimeUnit.MINUTES.toMillis(30)); // Cap at 30 minutes
        }

        return delayMs;
    }

    @Override
    public boolean shouldSendToDLQ(String errorType, int retryCount) {
        return retryCount >= maxRetries;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public String getName() {
        return exponentialBackoff ? "EXPONENTIAL_BACKOFF" : "FIXED_DELAY";
    }

    @Override
    public String getDescription() {
        if (exponentialBackoff) {
            return "Exponential backoff - " + maxRetries + " retries, initial delay " + delayMs + "ms";
        } else {
            return "Fixed delay - " + maxRetries + " retries, " + delayMs + "ms between attempts";
        }
    }

    /**
     * Gets the fixed delay in milliseconds.
     *
     * @return the delay
     */
    public long getDelayMs() {
        return delayMs;
    }

    /**
     * Checks if exponential backoff is enabled.
     *
     * @return true if exponential backoff
     */
    public boolean isExponentialBackoff() {
        return exponentialBackoff;
    }

    @Override
    public String toString() {
        return "FixedDelayRetryPolicy{name='" + getName() + "', maxRetries=" + maxRetries + ", delayMs=" + delayMs + '}';
    }
}
