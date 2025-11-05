package com.droid.bss.infrastructure.event.publisher;

import java.time.Instant;
import java.util.Objects;

/**
 * Result of an asynchronous event publishing operation.
 *
 * @since 1.0
 */
public class EventPublishResult {

    private final boolean success;
    private final String eventId;
    private final String topic;
    private final String errorMessage;
    private final Instant publishedAt;
    private final String partitionKey;
    private final int partition;
    private final long offset;

    private EventPublishResult(Builder builder) {
        this.success = builder.success;
        this.eventId = builder.eventId;
        this.topic = builder.topic;
        this.errorMessage = builder.errorMessage;
        this.publishedAt = builder.publishedAt;
        this.partitionKey = builder.partitionKey;
        this.partition = builder.partition;
        this.offset = builder.offset;
    }

    /**
     * Creates a successful publish result.
     *
     * @param eventId the event ID
     * @param topic the topic where event was published
     * @param publishedAt the time when event was published
     * @param partitionKey the partition key used
     * @param partition the partition number
     * @param offset the offset in the partition
     * @return the publish result
     */
    public static EventPublishResult success(String eventId, String topic, Instant publishedAt,
                                             String partitionKey, int partition, long offset) {
        return builder()
            .success(true)
            .eventId(eventId)
            .topic(topic)
            .publishedAt(publishedAt)
            .partitionKey(partitionKey)
            .partition(partition)
            .offset(offset)
            .build();
    }

    /**
     * Creates a failed publish result.
     *
     * @param eventId the event ID
     * @param topic the topic where event was being published
     * @param errorMessage the error message
     * @return the publish result
     */
    public static EventPublishResult failure(String eventId, String topic, String errorMessage) {
        return builder()
            .success(false)
            .eventId(eventId)
            .topic(topic)
            .errorMessage(errorMessage)
            .publishedAt(Instant.now())
            .build();
    }

    /**
     * Creates a failed publish result.
     *
     * @param eventId the event ID
     * @param topic the topic where event was being published
     * @param errorMessage the error message
     * @param publishedAt the time when publishing was attempted
     * @return the publish result
     */
    public static EventPublishResult failure(String eventId, String topic, String errorMessage, Instant publishedAt) {
        return builder()
            .success(false)
            .eventId(eventId)
            .topic(topic)
            .errorMessage(errorMessage)
            .publishedAt(publishedAt)
            .build();
    }

    /**
     * Checks if the publish operation was successful.
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
     * Gets the topic.
     *
     * @return the topic
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Gets the error message if publishing failed.
     *
     * @return the error message (may be null)
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the time when the event was published.
     *
     * @return the published timestamp
     */
    public Instant getPublishedAt() {
        return publishedAt;
    }

    /**
     * Gets the partition key.
     *
     * @return the partition key (may be null)
     */
    public String getPartitionKey() {
        return partitionKey;
    }

    /**
     * Gets the partition number.
     *
     * @return the partition number
     */
    public int getPartition() {
        return partition;
    }

    /**
     * Gets the offset.
     *
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Creates a builder for EventPublishResult.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for EventPublishResult.
     */
    public static class Builder {
        private boolean success;
        private String eventId;
        private String topic;
        private String errorMessage;
        private Instant publishedAt;
        private String partitionKey;
        private int partition = -1;
        private long offset = -1;

        private Builder() {}

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder publishedAt(Instant publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder partitionKey(String partitionKey) {
            this.partitionKey = partitionKey;
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

        public EventPublishResult build() {
            return new EventPublishResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventPublishResult that = (EventPublishResult) o;
        return success == that.success &&
            partition == that.partition &&
            offset == that.offset &&
            Objects.equals(eventId, that.eventId) &&
            Objects.equals(topic, that.topic) &&
            Objects.equals(errorMessage, that.errorMessage) &&
            Objects.equals(publishedAt, that.publishedAt) &&
            Objects.equals(partitionKey, that.partitionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, eventId, topic, errorMessage, publishedAt, partitionKey, partition, offset);
    }

    @Override
    public String toString() {
        return "EventPublishResult{" +
            "success=" + success +
            ", eventId='" + eventId + '\'' +
            ", topic='" + topic + '\'' +
            ", errorMessage='" + errorMessage + '\'' +
            ", publishedAt=" + publishedAt +
            ", partitionKey='" + partitionKey + '\'' +
            ", partition=" + partition +
            ", offset=" + offset +
            '}';
    }
}
