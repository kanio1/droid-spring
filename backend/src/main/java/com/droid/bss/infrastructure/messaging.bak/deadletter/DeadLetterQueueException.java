package com.droid.bss.infrastructure.messaging.deadletter;

/**
 * Exception thrown when DLQ operations fail.
 *
 * @since 1.0
 */
public class DeadLetterQueueException extends Exception {

    private final String messageId;
    private final String queueName;
    private final String errorCode;
    private final boolean retryable;

    /**
     * Creates a new DeadLetterQueueException.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public DeadLetterQueueException(String message, Throwable cause) {
        super(message, cause);
        this.messageId = null;
        this.queueName = null;
        this.errorCode = "DLQ_ERROR";
        this.retryable = false;
    }

    /**
     * Creates a new DeadLetterQueueException.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param message the error message
     * @param cause the underlying cause
     */
    public DeadLetterQueueException(String messageId, String queueName, String message, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
        this.queueName = queueName;
        this.errorCode = "DLQ_ERROR";
        this.retryable = false;
    }

    /**
     * Creates a new DeadLetterQueueException.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param message the error message
     * @param errorCode the error code
     * @param retryable whether the error is retryable
     * @param cause the underlying cause
     */
    public DeadLetterQueueException(String messageId, String queueName, String message,
                                    String errorCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.messageId = messageId;
        this.queueName = queueName;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * Creates a new DeadLetterQueueException.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param message the error message
     * @param errorCode the error code
     * @param retryable whether the error is retryable
     */
    public DeadLetterQueueException(String messageId, String queueName, String message,
                                    String errorCode, boolean retryable) {
        super(message);
        this.messageId = messageId;
        this.queueName = queueName;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * Creates an exception for message not found.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @return the exception
     */
    public static DeadLetterQueueException messageNotFound(String messageId, String queueName) {
        return new DeadLetterQueueException(
            messageId,
            queueName,
            "Message not found in DLQ: " + messageId,
            "MESSAGE_NOT_FOUND",
            false,
            null
        );
    }

    /**
     * Creates an exception for queue full.
     *
     * @param queueName the queue name
     * @param currentSize the current queue size
     * @param maxSize the maximum queue size
     * @return the exception
     */
    public static DeadLetterQueueException queueFull(String queueName, long currentSize, long maxSize) {
        return new DeadLetterQueueException(
            null,
            queueName,
            "Queue is full: " + currentSize + "/" + maxSize,
            "QUEUE_FULL",
            false,
            null
        );
    }

    /**
     * Creates an exception for serialization failure.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param cause the underlying cause
     * @return the exception
     */
    public static DeadLetterQueueException serializationFailure(String messageId, String queueName, Throwable cause) {
        return new DeadLetterQueueException(
            messageId,
            queueName,
            "Failed to serialize message: " + cause.getMessage(),
            "SERIALIZATION_FAILED",
            false,
            cause
        );
    }

    /**
     * Creates an exception for deserialization failure.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param cause the underlying cause
     * @return the exception
     */
    public static DeadLetterQueueException deserializationFailure(String messageId, String queueName, Throwable cause) {
        return new DeadLetterQueueException(
            messageId,
            queueName,
            "Failed to deserialize message: " + cause.getMessage(),
            "DESERIALIZATION_FAILED",
            false,
            cause
        );
    }

    /**
     * Creates an exception for topic not found.
     *
     * @param topic the topic name
     * @param queueName the queue name
     * @return the exception
     */
    public static DeadLetterQueueException topicNotFound(String topic, String queueName) {
        return new DeadLetterQueueException(
            null,
            queueName,
            "Topic not found: " + topic,
            "TOPIC_NOT_FOUND",
            false,
            null
        );
    }

    /**
     * Creates an exception for invalid entry.
     *
     * @param queueName the queue name
     * @param message the error message
     * @return the exception
     */
    public static DeadLetterQueueException invalidEntry(String queueName, String message) {
        return new DeadLetterQueueException(
            null,
            queueName,
            "Invalid DLQ entry: " + message,
            "INVALID_ENTRY",
            false,
            null
        );
    }

    /**
     * Creates an exception for connection failure.
     *
     * @param queueName the queue name
     * @param message the error message
     * @param cause the underlying cause
     * @return the exception
     */
    public static DeadLetterQueueException connectionFailure(String queueName, String message, Throwable cause) {
        return new DeadLetterQueueException(
            null,
            queueName,
            "Connection failure: " + message,
            "CONNECTION_FAILED",
            true,
            cause
        );
    }

    /**
     * Creates an exception for requeue failure.
     *
     * @param messageId the message ID
     * @param queueName the queue name
     * @param cause the underlying cause
     * @return the exception
     */
    public static DeadLetterQueueException requeueFailure(String messageId, String queueName, Throwable cause) {
        return new DeadLetterQueueException(
            messageId,
            queueName,
            "Failed to requeue message: " + cause.getMessage(),
            "REQUEUE_FAILED",
            true,
            cause
        );
    }

    /**
     * Creates an exception for batch operation failure.
     *
     * @param queueName the queue name
     * @param operation the operation name
     * @param batchSize the batch size
     * @param successCount the number of successful operations
     * @param cause the underlying cause
     * @return the exception
     */
    public static DeadLetterQueueException batchOperationFailure(String queueName, String operation,
                                                                 int batchSize, int successCount, Throwable cause) {
        return new DeadLetterQueueException(
            null,
            queueName,
            String.format("Batch %s failed: %d/%d succeeded", operation, successCount, batchSize),
            "BATCH_OPERATION_FAILED",
            false,
            cause
        );
    }

    /**
     * Creates an exception for timeout.
     *
     * @param queueName the queue name
     * @param timeoutMs the timeout in milliseconds
     * @return the exception
     */
    public static DeadLetterQueueException timeout(String queueName, long timeoutMs) {
        return new DeadLetterQueueException(
            null,
            queueName,
            "Operation timed out after " + timeoutMs + "ms",
            "OPERATION_TIMEOUT",
            true,
            null
        );
    }

    /**
     * Gets the message ID.
     *
     * @return the message ID (may be null)
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the queue name.
     *
     * @return the queue name (may be null)
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Checks if the error is retryable.
     *
     * @return true if retryable
     */
    public boolean isRetryable() {
        return retryable;
    }
}
