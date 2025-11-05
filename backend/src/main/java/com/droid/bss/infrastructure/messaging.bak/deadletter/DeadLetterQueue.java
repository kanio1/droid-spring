package com.droid.bss.infrastructure.messaging.deadletter;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Dead Letter Queue (DLQ) operations.
 *
 * Handles storage and management of failed messages.
 *
 * @since 1.0
 */
public interface DeadLetterQueue {

    /**
     * Sends a message to the DLQ.
     *
     * @param entry the DLQ entry
     * @throws DeadLetterQueueException if sending fails
     */
    void send(DLQEntry entry) throws DeadLetterQueueException;

    /**
     * Sends a message to the DLQ asynchronously.
     *
     * @param entry the DLQ entry
     * @return the CompletableFuture
     */
    CompletableFuture<Void> sendAsync(DLQEntry entry);

    /**
     * Sends multiple messages to the DLQ.
     *
     * @param entries the list of entries
     * @throws DeadLetterQueueException if sending fails
     */
    void sendBatch(List<DLQEntry> entries) throws DeadLetterQueueException;

    /**
     * Sends multiple messages to the DLQ asynchronously.
     *
     * @param entries the list of entries
     * @return the CompletableFuture
     */
    CompletableFuture<Void> sendBatchAsync(List<DLQEntry> entries);

    /**
     * Retrieves a message from the DLQ by ID.
     *
     * @param messageId the message ID
     * @return the DLQ entry (may be null)
     * @throws DeadLetterQueueException if retrieval fails
     */
    DLQEntry get(String messageId) throws DeadLetterQueueException;

    /**
     * Retrieves multiple messages from the DLQ.
     *
     * @param limit the maximum number of messages to retrieve
     * @return the list of entries
     * @throws DeadLetterQueueException if retrieval fails
     */
    List<DLQEntry> getBatch(int limit) throws DeadLetterQueueException;

    /**
     * Retrieves messages for a specific topic.
     *
     * @param topic the topic name
     * @param limit the maximum number of messages
     * @return the list of entries
     * @throws DeadLetterQueueException if retrieval fails
     */
    List<DLQEntry> getByTopic(String topic, int limit) throws DeadLetterQueueException;

    /**
     * Retrieves messages for a specific error type.
     *
     * @param errorType the error type
     * @param limit the maximum number of messages
     * @return the list of entries
     * @throws DeadLetterQueueException if retrieval fails
     */
    List<DLQEntry> getByErrorType(String errorType, int limit) throws DeadLetterQueueException;

    /**
     * Retrieves messages that occurred after a specific timestamp.
     *
     * @param timestamp the timestamp
     * @param limit the maximum number of messages
     * @return the list of entries
     * @throws DeadLetterQueueException if retrieval fails
     */
    List<DLQEntry> getAfterTimestamp(Instant timestamp, int limit) throws DeadLetterQueueException;

    /**
     * Deletes a message from the DLQ.
     *
     * @param messageId the message ID
     * @return true if deleted
     * @throws DeadLetterQueueException if deletion fails
     */
    boolean delete(String messageId) throws DeadLetterQueueException;

    /**
     * Deletes multiple messages from the DLQ.
     *
     * @param messageIds the list of message IDs
     * @return the number of deleted messages
     * @throws DeadLetterQueueException if deletion fails
     */
    int deleteBatch(List<String> messageIds) throws DeadLetterQueueException;

    /**
     * Requeues a message back to the original topic.
     *
     * @param messageId the message ID
     * @return true if requeued
     * @throws DeadLetterQueueException if requeue fails
     */
    boolean requeue(String messageId) throws DeadLetterQueueException;

    /**
     * Requeues multiple messages back to their original topics.
     *
     * @param messageIds the list of message IDs
     * @return the number of requeued messages
     * @throws DeadLetterQueueException if requeue fails
     */
    int requeueBatch(List<String> messageIds) throws DeadLetterQueueException;

    /**
     * Counts the number of messages in the DLQ.
     *
     * @return the count
     * @throws DeadLetterQueueException if counting fails
     */
    long count() throws DeadLetterQueueException;

    /**
     * Counts the number of messages for a specific topic.
     *
     * @param topic the topic name
     * @return the count
     * @throws DeadLetterQueueException if counting fails
     */
    long countByTopic(String topic) throws DeadLetterQueueException;

    /**
     * Counts the number of messages for a specific error type.
     *
     * @param errorType the error type
     * @return the count
     * @throws DeadLetterQueueException if counting fails
     */
    long countByErrorType(String errorType) throws DeadLetterQueueException;

    /**
     * Checks if the DLQ is healthy.
     *
     * @return true if healthy
     */
    boolean isHealthy();

    /**
     * Gets the DLQ statistics.
     *
     * @return the statistics
     */
    DLQStats getStats();

    /**
     * Purges all messages from the DLQ.
     *
     * @return the number of purged messages
     * @throws DeadLetterQueueException if purging fails
     */
    long purge() throws DeadLetterQueueException;

    /**
     * Purges messages older than the specified timestamp.
     *
     * @param timestamp the timestamp
     * @return the number of purged messages
     * @throws DeadLetterQueueException if purging fails
     */
    long purgeOlderThan(Instant timestamp) throws DeadLetterQueueException;

    /**
     * Gets the queue name.
     *
     * @return the queue name
     */
    String getName();

    /**
     * Gets the queue configuration.
     *
     * @return the configuration
     */
    DeadLetterQueueConfig getConfig();

    /**
     * Closes the DLQ.
     *
     * @throws DeadLetterQueueException if closing fails
     */
    void close() throws DeadLetterQueueException;
}
