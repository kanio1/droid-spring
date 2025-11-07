package com.droid.bss.infrastructure.event.sourcing;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a stored event in the event store
 */
public class StoredEvent {
    private final UUID id;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String eventData;
    private final LocalDateTime timestamp;
    private final String userId;
    private final String correlationId;
    private final long version;

    public StoredEvent(
            UUID id,
            String aggregateId,
            String aggregateType,
            String eventType,
            String eventData,
            LocalDateTime timestamp,
            String userId,
            String correlationId,
            long version) {
        this.id = Objects.requireNonNull(id, "Event ID cannot be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID cannot be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "Aggregate type cannot be null");
        this.eventType = Objects.requireNonNull(eventType, "Event type cannot be null");
        this.eventData = Objects.requireNonNull(eventData, "Event data cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.userId = userId;
        this.correlationId = correlationId;
        this.version = version;
    }

    public static StoredEvent fromDomainEvent(
            UUID eventId,
            String aggregateId,
            String aggregateType,
            String eventType,
            String eventData,
            String userId,
            String correlationId,
            long version) {
        return new StoredEvent(
                eventId,
                aggregateId,
                aggregateType,
                eventType,
                eventData,
                LocalDateTime.now(),
                userId,
                correlationId,
                version
        );
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredEvent that = (StoredEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StoredEvent{" +
                "id=" + id +
                ", aggregateId='" + aggregateId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                '}';
    }
}
