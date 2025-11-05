package com.droid.bss.infrastructure.event.handlers;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of an event handling operation.
 *
 * @since 1.0
 */
public class EventHandlingResult {

    private final boolean success;
    private final String eventId;
    private final String eventType;
    private final String handlerName;
    private final String message;
    private final Instant processedAt;
    private final long processingTimeMs;
    private final int retryCount;

    private EventHandlingResult(Builder builder) {
        this.success = builder.success;
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.handlerName = builder.handlerName;
        this.message = builder.message;
        this.processedAt = builder.processedAt;
        this.processingTimeMs = builder.processingTimeMs;
        this.retryCount = builder.retryCount;
    }

    /**
     * Creates a successful handling result.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param processingTimeMs the processing time in milliseconds
     * @return the result
     */
    public static EventHandlingResult success(String eventId, String eventType, String handlerName,
                                              long processingTimeMs) {
        return builder()
            .success(true)
            .eventId(eventId)
            .eventType(eventType)
            .handlerName(handlerName)
            .processedAt(Instant.now())
            .processingTimeMs(processingTimeMs)
            .retryCount(0)
            .build();
    }

    /**
     * Creates a failed handling result.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param processingTimeMs the processing time in milliseconds
     * @return the result
     */
    public static EventHandlingResult failure(String eventId, String eventType, String handlerName,
                                              String message, long processingTimeMs) {
        return builder()
            .success(false)
            .eventId(eventId)
            .eventType(eventType)
            .handlerName(handlerName)
            .message(message)
            .processedAt(Instant.now())
            .processingTimeMs(processingTimeMs)
            .retryCount(0)
            .build();
    }

    /**
     * Creates a successful handling result with retry count.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param processingTimeMs the processing time in milliseconds
     * @param retryCount the number of retries
     * @return the result
     */
    public static EventHandlingResult success(String eventId, String eventType, String handlerName,
                                              long processingTimeMs, int retryCount) {
        return builder()
            .success(true)
            .eventId(eventId)
            .eventType(eventType)
            .handlerName(handlerName)
            .processedAt(Instant.now())
            .processingTimeMs(processingTimeMs)
            .retryCount(retryCount)
            .build();
    }

    /**
     * Creates a failed handling result with retry count.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param processingTimeMs the processing time in milliseconds
     * @param retryCount the number of retries
     * @return the result
     */
    public static EventHandlingResult failure(String eventId, String eventType, String handlerName,
                                              String message, long processingTimeMs, int retryCount) {
        return builder()
            .success(false)
            .eventId(eventId)
            .eventType(eventType)
            .handlerName(handlerName)
            .message(message)
            .processedAt(Instant.now())
            .processingTimeMs(processingTimeMs)
            .retryCount(retryCount)
            .build();
    }

    /**
     * Checks if the handling was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the event ID.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the event type.
     *
     * @return the event type
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the handler name.
     *
     * @return the handler name
     */
    public String getHandlerName() {
        return handlerName;
    }

    /**
     * Gets the message (error message if failed).
     *
     * @return the message (may be null)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the time when the event was processed.
     *
     * @return the processed timestamp
     */
    public Instant getProcessedAt() {
        return processedAt;
    }

    /**
     * Gets the processing time in milliseconds.
     *
     * @return the processing time
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Gets the number of retries.
     *
     * @return the retry count
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Creates a builder for EventHandlingResult.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventHandlingResult.
     */
    public static class Builder {
        private boolean success;
        private String eventId;
        private String eventType;
        private String handlerName;
        private String message;
        private Instant processedAt;
        private long processingTimeMs;
        private int retryCount;

        private Builder() {}

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder handlerName(String handlerName) {
            this.handlerName = handlerName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder processedAt(Instant processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public EventHandlingResult build() {
            return new EventHandlingResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventHandlingResult that = (EventHandlingResult) o;
        return success == that.success &&
            processingTimeMs == that.processingTimeMs &&
            retryCount == that.retryCount &&
            Objects.equals(eventId, that.eventId) &&
            Objects.equals(eventType, that.eventType) &&
            Objects.equals(handlerName, that.handlerName) &&
            Objects.equals(message, that.message) &&
            Objects.equals(processedAt, that.processedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, eventId, eventType, handlerName, message, processedAt, processingTimeMs, retryCount);
    }

    @Override
    public String toString() {
        return "EventHandlingResult{" +
            "success=" + success +
            ", eventId='" + eventId + '\'' +
            ", eventType='" + eventType + '\'' +
            ", handlerName='" + handlerName + '\'' +
            ", message='" + message + '\'' +
            ", processingTimeMs=" + processingTimeMs +
            ", retryCount=" + retryCount +
            '}';
    }
}
