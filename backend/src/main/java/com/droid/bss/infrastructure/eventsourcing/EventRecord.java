package com.droid.bss.infrastructure.eventsourcing;

import java.time.Instant;
import java.util.Map;

/**
 * Event record for storing in Redis Streams
 */
public class EventRecord {
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private DomainEvent eventData;
    private Instant timestamp;
    private int version;

    public EventRecord() {}

    private EventRecord(Builder builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.aggregateType = builder.aggregateType;
        this.aggregateId = builder.aggregateId;
        this.eventData = builder.eventData;
        this.timestamp = builder.timestamp;
        this.version = builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EventRecord fromMap(Map<String, Object> map) {
        Builder builder = new Builder();
        builder.eventId = (String) map.get("eventId");
        builder.eventType = (String) map.get("eventType");
        builder.aggregateType = (String) map.get("aggregateType");
        builder.aggregateId = (String) map.get("aggregateId");
        builder.timestamp = Instant.parse((String) map.get("timestamp"));
        builder.version = (Integer) map.get("version");
        // eventData would be deserialized based on eventType in production
        return builder.build();
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "eventId", eventId,
            "eventType", eventType,
            "aggregateType", aggregateType,
            "aggregateId", aggregateId,
            "timestamp", timestamp.toString(),
            "version", version
        );
    }

    public static class Builder {
        private String eventId;
        private String eventType;
        private String aggregateType;
        private String aggregateId;
        private DomainEvent eventData;
        private Instant timestamp;
        private int version;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder eventData(DomainEvent eventData) {
            this.eventData = eventData;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public EventRecord build() {
            return new EventRecord(this);
        }
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public DomainEvent getEventData() { return eventData; }
    public Instant getTimestamp() { return timestamp; }
    public int getVersion() { return version; }

    // Setters
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public void setEventData(DomainEvent eventData) { this.eventData = eventData; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public void setVersion(int version) { this.version = version; }
}
