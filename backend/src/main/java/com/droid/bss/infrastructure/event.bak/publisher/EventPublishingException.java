package com.droid.bss.infrastructure.event.publisher;

import java.util.UUID;

/**
 * Exception thrown when event publishing fails.
 *
 * @since 1.0
 */
public class EventPublishingException extends RuntimeException {

    private final String eventId;
    private final String eventType;
    private final String topic;
    private final String errorCode;

    /**
     * Creates a new EventPublishingException.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
        this.topic = null;
        this.errorCode = "EVENT_PUBLISHING_ERROR";
    }

    /**
     * Creates a new EventPublishingException.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param message the error message
     * @param cause the underlying cause
     */
    public EventPublishingException(String eventId, String eventType, String topic, String message, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.errorCode = "EVENT_PUBLISHING_ERROR";
    }

    /**
     * Creates a new EventPublishingException.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param message the error message
     */
    public EventPublishingException(String eventId, String eventType, String topic, String message) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.errorCode = "EVENT_PUBLISHING_ERROR";
    }

    /**
     * Creates a new EventPublishingException with specific error code.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param message the error message
     * @param errorCode the error code
     * @param cause the underlying cause
     */
    public EventPublishingException(String eventId, String eventType, String topic,
                                    String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.errorCode = errorCode;
    }

    /**
     * Creates a new EventPublishingException with specific error code.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param message the error message
     * @param errorCode the error code
     */
    public EventPublishingException(String eventId, String eventType, String topic,
                                    String message, String errorCode) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
        this.topic = topic;
        this.errorCode = errorCode;
    }

    /**
     * Creates an exception for serialization failure.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param message the error message
     * @return the exception
     */
    public static EventPublishingException serializationFailure(String eventId, String eventType,
                                                                String topic, String message) {
        return new EventPublishingException(
            eventId,
            eventType,
            topic,
            "Failed to serialize event: " + message,
            "SERIALIZATION_ERROR",
            null
        );
    }

    /**
     * Creates an exception for topic not found.
     *
     * @param topic the topic
     * @return the exception
     */
    public static EventPublishingException topicNotFound(String topic) {
        return new EventPublishingException(
            null,
            null,
            topic,
            "Topic not found: " + topic,
            "TOPIC_NOT_FOUND",
            null
        );
    }

    /**
     * Creates an exception for Kafka delivery failure.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param topic the topic
     * @param cause the underlying cause
     * @return the exception
     */
    public static EventPublishingException kafkaDeliveryFailure(String eventId, String eventType,
                                                                String topic, Throwable cause) {
        return new EventPublishingException(
            eventId,
            eventType,
            topic,
            "Failed to deliver event to Kafka: " + cause.getMessage(),
            "KAFKA_DELIVERY_FAILED",
            cause
        );
    }

    /**
     * Creates an exception for invalid event.
     *
     * @param message the error message
     * @return the exception
     */
    public static EventPublishingException invalidEvent(String message) {
        return new EventPublishingException(
            null,
            null,
            null,
            "Invalid event: " + message,
            "INVALID_EVENT",
            null
        );
    }

    /**
     * Creates an exception for connection failure.
     *
     * @param topic the topic
     * @param cause the underlying cause
     * @return the exception
     */
    public static EventPublishingException connectionFailure(String topic, Throwable cause) {
        return new EventPublishingException(
            null,
            null,
            topic,
            "Failed to connect to message broker: " + cause.getMessage(),
            "CONNECTION_FAILED",
            cause
        );
    }

    /**
     * Creates an exception for batch publishing failure.
     *
     * @param totalEvents the total number of events
     * @param successCount the number of successful events
     * @param cause the underlying cause
     * @return the exception
     */
    public static EventPublishingException batchPublishingFailure(int totalEvents, int successCount,
                                                                  Throwable cause) {
        return new EventPublishingException(
            null,
            null,
            null,
            String.format("Batch publishing failed: %d/%d events published successfully",
                successCount, totalEvents),
            "BATCH_PUBLISHING_FAILED",
            cause
        );
    }

    /**
     * Gets the event ID.
     *
     * @return the event ID (may be null)
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Gets the event type.
     *
     * @return the event type (may be null)
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the topic.
     *
     * @return the topic (may be null)
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
