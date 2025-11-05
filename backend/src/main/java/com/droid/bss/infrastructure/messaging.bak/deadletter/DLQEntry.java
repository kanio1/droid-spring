package com.droid.bss.infrastructure.messaging.deadletter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an entry in the Dead Letter Queue.
 *
 * @since 1.0
 */
public class DLQEntry {

    private final String messageId;
    private final String topic;
    private final int partition;
    private final long offset;
    private final String errorMessage;
    private final String errorCode;
    private final String errorType;
    private final int retryCount;
    private final Instant timestamp;
    private final Instant addedAt;
    private final String originalPayload;
    private final Map<String, Object> headers;
    private final String originalMessageKey;
    private final String exceptionType;
    private final String stackTrace;

    private DLQEntry(Builder builder) {
        this.messageId = builder.messageId != null ? builder.messageId : UUID.randomUUID().toString();
        this.topic = builder.topic;
        this.partition = builder.partition;
        this.offset = builder.offset;
        this.errorMessage = builder.errorMessage;
        this.errorCode = builder.errorCode;
        this.errorType = builder.errorType;
        this.retryCount = builder.retryCount;
        this.timestamp = builder.timestamp;
        this.addedAt = Instant.now();
        this.originalPayload = builder.originalPayload;
        this.headers = Map.copyOf(builder.headers != null ? builder.headers : new HashMap<>());
        this.originalMessageKey = builder.originalMessageKey;
        this.exceptionType = builder.exceptionType;
        this.stackTrace = builder.stackTrace;
    }

    /**
     * Creates a new Builder for DLQEntry.
     *
     * @return the builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a new DLQEntry with required fields.
     *
     * @param topic the topic
     * @param errorMessage the error message
     * @param errorType the error type
     * @return the entry
     */
    public static DLQEntry create(String topic, String errorMessage, String errorType) {
        return newBuilder()
            .topic(topic)
            .errorMessage(errorMessage)
            .errorType(errorType)
            .build();
    }

    /**
     * Creates a new DLQEntry from a failed message.
     *
     * @param topic the topic
     * @param errorMessage the error message
     * @param errorType the error type
     * @param payload the original payload
     * @return the entry
     */
    public static DLQEntry fromFailedMessage(String topic, String errorMessage, String errorType, String payload) {
        return newBuilder()
            .topic(topic)
            .errorMessage(errorMessage)
            .errorType(errorType)
            .originalPayload(payload)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Creates a new DLQEntry with exception details.
     *
     * @param topic the topic
     * @param errorMessage the error message
     * @param errorType the error type
     * @param payload the original payload
     * @param exception the exception
     * @return the entry
     */
    public static DLQEntry withException(String topic, String errorMessage, String errorType,
                                        String payload, Throwable exception) {
        return newBuilder()
            .topic(topic)
            .errorMessage(errorMessage)
            .errorType(errorType)
            .originalPayload(payload)
            .exception(exception.getClass().getName())
            .stackTrace(getStackTrace(exception))
            .timestamp(Instant.now())
            .build();
    }

    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Builder for DLQEntry.
     */
    public static class Builder {
        private String messageId;
        private String topic;
        private int partition = -1;
        private long offset = -1;
        private String errorMessage;
        private String errorCode;
        private String errorType;
        private int retryCount = 0;
        private Instant timestamp = Instant.now();
        private String originalPayload;
        private Map<String, Object> headers = new HashMap<>();
        private String originalMessageKey;
        private String exceptionType;
        private String stackTrace;

        private Builder() {}

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder partition(int partition) {
            this.partition = partition;
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder originalPayload(String originalPayload) {
            this.originalPayload = originalPayload;
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers = headers != null ? headers : new HashMap<>();
            return this;
        }

        public Builder addHeader(String key, Object value) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.put(key, value);
            return this;
        }

        public Builder originalMessageKey(String originalMessageKey) {
            this.originalMessageKey = originalMessageKey;
            return this;
        }

        public Builder exceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public DLQEntry build() {
            if (topic == null || topic.isBlank()) {
                throw new IllegalArgumentException("Topic cannot be null or blank");
            }
            if (errorMessage == null || errorMessage.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be null or blank");
            }
            if (errorType == null || errorType.isBlank()) {
                throw new IllegalArgumentException("Error type cannot be null or blank");
            }
            return new DLQEntry(this);
        }
    }

    // Getters

    public String getMessageId() {
        return messageId;
    }

    public String getTopic() {
        return topic;
    }

    public int getPartition() {
        return partition;
    }

    public long getOffset() {
        return offset;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public String getOriginalPayload() {
        return originalPayload;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public String getOriginalMessageKey() {
        return originalMessageKey;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Gets the age of the message in milliseconds.
     *
     * @return the age in milliseconds
     */
    public long getAgeMs() {
        return java.time.Duration.between(timestamp, Instant.now()).toMillis();
    }

    /**
     * Gets the time since the message was added to DLQ in milliseconds.
     *
     * @return the time since addition in milliseconds
     */
    public long getTimeInDLQMs() {
        return java.time.Duration.between(addedAt, Instant.now()).toMillis();
    }

    /**
     * Checks if the entry has a header with the given key.
     *
     * @param key the header key
     * @return true if header exists
     */
    public boolean hasHeader(String key) {
        return headers.containsKey(key);
    }

    /**
     * Gets a header value by key.
     *
     * @param key the header key
     * @return the header value (may be null)
     */
    public Object getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Gets a header value by key with default.
     *
     * @param key the header key
     * @param defaultValue the default value
     * @return the header value or default
     */
    public Object getHeader(String key, Object defaultValue) {
        return headers.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DLQEntry that = (DLQEntry) o;
        return Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "DLQEntry{" +
            "messageId='" + messageId + '\'' +
            ", topic='" + topic + '\'' +
            ", partition=" + partition +
            ", offset=" + offset +
            ", errorType='" + errorType + '\'' +
            ", retryCount=" + retryCount +
            ", ageMs=" + getAgeMs() +
            '}';
    }
}
