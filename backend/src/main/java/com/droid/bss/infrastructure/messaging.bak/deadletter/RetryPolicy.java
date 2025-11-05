package com.droid.bss.infrastructure.messaging.deadletter;

/**
 * Policy for determining retry behavior.
 *
 * @since 1.0
 */
public interface RetryPolicy {

    /**
     * Checks if a message should be retried.
     *
     * @param entry the DLQ entry
     * @return true if should retry
     */
    boolean shouldRetry(DLQEntry entry);

    /**
     * Gets the delay before the next retry.
     *
     * @param entry the DLQ entry
     * @return the delay in milliseconds
     */
    long getRetryDelay(DLQEntry entry);

    /**
     * Checks if a message should be sent to DLQ.
     *
     * @param errorType the error type
     * @param retryCount the current retry count
     * @return true if should send to DLQ
     */
    boolean shouldSendToDLQ(String errorType, int retryCount);

    /**
     * Gets the maximum number of retries.
     *
     * @return the maximum retries
     */
    int getMaxRetries();

    /**
     * Gets the policy name.
     *
     * @return the policy name
     */
    String getName();

    /**
     * Gets the policy description.
     *
     * @return the description
     */
    String getDescription();
}
