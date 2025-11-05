package com.droid.bss.infrastructure.event.handlers;

/**
 * Exception thrown when event handling fails.
 *
 * @since 1.0
 */
public class EventHandlingException extends Exception {

    private final String eventId;
    private final String eventType;
    private final String handlerName;
    private final String errorCode;
    private final boolean retryable;

    /**
     * Creates a new EventHandlingException.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public EventHandlingException(String message, Throwable cause) {
        super(message, cause);
        this.eventId = null;
        this.eventType = null;
        this.handlerName = null;
        this.errorCode = "EVENT_HANDLING_ERROR";
        this.retryable = false;
    }

    /**
     * Creates a new EventHandlingException.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param cause the underlying cause
     */
    public EventHandlingException(String eventId, String eventType, String handlerName,
                                  String message, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
        this.handlerName = handlerName;
        this.errorCode = "EVENT_HANDLING_ERROR";
        this.retryable = false;
    }

    /**
     * Creates a new EventHandlingException.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     */
    public EventHandlingException(String eventId, String eventType, String handlerName, String message) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
        this.handlerName = handlerName;
        this.errorCode = "EVENT_HANDLING_ERROR";
        this.retryable = false;
    }

    /**
     * Creates a new EventHandlingException with specific error code.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param errorCode the error code
     * @param retryable whether the error is retryable
     * @param cause the underlying cause
     */
    public EventHandlingException(String eventId, String eventType, String handlerName,
                                  String message, String errorCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.eventId = eventId;
        this.eventType = eventType;
        this.handlerName = handlerName;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * Creates a new EventHandlingException with specific error code.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param errorCode the error code
     * @param retryable whether the error is retryable
     */
    public EventHandlingException(String eventId, String eventType, String handlerName,
                                  String message, String errorCode, boolean retryable) {
        super(message);
        this.eventId = eventId;
        this.eventType = eventType;
        this.handlerName = handlerName;
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    /**
     * Creates an exception for handler not found.
     *
     * @param eventType the event type
     * @return the exception
     */
    public static EventHandlingException handlerNotFound(String eventType) {
        return new EventHandlingException(
            null,
            eventType,
            null,
            "No handler found for event type: " + eventType,
            "HANDLER_NOT_FOUND",
            false,
            null
        );
    }

    /**
     * Creates an exception for deserialization failure.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param message the error message
     * @return the exception
     */
    public static EventHandlingException deserializationFailure(String eventId, String eventType, String message) {
        return new EventHandlingException(
            eventId,
            eventType,
            null,
            "Failed to deserialize event: " + message,
            "DESERIALIZATION_FAILED",
            false,
            null
        );
    }

    /**
     * Creates an exception for validation failure.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param message the error message
     * @return the exception
     */
    public static EventHandlingException validationFailed(String eventId, String eventType, String message) {
        return new EventHandlingException(
            eventId,
            eventType,
            null,
            "Event validation failed: " + message,
            "VALIDATION_FAILED",
            false,
            null
        );
    }

    /**
     * Creates a retryable exception.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param cause the underlying cause
     * @return the exception
     */
    public static EventHandlingException retryableError(String eventId, String eventType, String handlerName,
                                                        String message, Throwable cause) {
        return new EventHandlingException(
            eventId,
            eventType,
            handlerName,
            "Retryable error: " + message,
            "RETRYABLE_ERROR",
            true,
            cause
        );
    }

    /**
     * Creates an exception for permanent failure.
     *
     * @param eventId the event ID
     * @param eventType the event type
     * @param handlerName the handler name
     * @param message the error message
     * @param cause the underlying cause
     * @return the exception
     */
    public static EventHandlingException permanentFailure(String eventId, String eventType, String handlerName,
                                                          String message, Throwable cause) {
        return new EventHandlingException(
            eventId,
            eventType,
            handlerName,
            "Permanent failure: " + message,
            "PERMANENT_FAILURE",
            false,
            cause
        );
    }

    /**
     * Creates an exception for invalid handler configuration.
     *
     * @param handlerName the handler name
     * @param message the error message
     * @return the exception
     */
    public static EventHandlingException invalidConfiguration(String handlerName, String message) {
        return new EventHandlingException(
            null,
            null,
            handlerName,
            "Invalid handler configuration: " + message,
            "INVALID_CONFIGURATION",
            false,
            null
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
     * Gets the handler name.
     *
     * @return the handler name (may be null)
     */
    public String getHandlerName() {
        return handlerName;
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
