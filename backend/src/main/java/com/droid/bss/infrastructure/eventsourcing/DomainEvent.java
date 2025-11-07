package com.droid.bss.infrastructure.eventsourcing;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base domain event for event sourcing
 */
public abstract class DomainEvent {
    protected String eventId;
    protected String eventType;
    protected Instant timestamp;
    protected int version;

    public DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public DomainEvent(String eventType) {
        this();
        this.eventType = eventType;
    }

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    // Convert to map for Redis storage
    public abstract Map<String, Object> toMap();

    // Create from map
    public static DomainEvent fromMap(String eventType, Map<String, Object> map) {
        // In production, would use polymorphism to instantiate correct event type
        return null; // Placeholder
    }
}
