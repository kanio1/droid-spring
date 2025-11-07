package com.droid.bss.infrastructure.event.sourcing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for storing events
 */
@Entity
@Table(name = "event_store")
public class EventEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "version", nullable = false)
    private long version;

    public EventEntity() {
    }

    public EventEntity(
            UUID id,
            String aggregateId,
            String aggregateType,
            String eventType,
            String eventData,
            LocalDateTime timestamp,
            String userId,
            String correlationId,
            long version) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.timestamp = timestamp;
        this.userId = userId;
        this.correlationId = correlationId;
        this.version = version;
    }

    public static EventEntity fromStoredEvent(
            com.droid.bss.infrastructure.event.sourcing.StoredEvent storedEvent) {
        return new EventEntity(
                storedEvent.getId(),
                storedEvent.getAggregateId(),
                storedEvent.getAggregateType(),
                storedEvent.getEventType(),
                storedEvent.getEventData(),
                storedEvent.getTimestamp(),
                storedEvent.getUserId(),
                storedEvent.getCorrelationId(),
                storedEvent.getVersion()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        this.eventData = eventData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventEntity that = (EventEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
