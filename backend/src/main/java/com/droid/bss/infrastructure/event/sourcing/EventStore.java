package com.droid.bss.infrastructure.event.sourcing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Store interface for managing events
 */
public interface EventStore {

    /**
     * Save events for an aggregate
     */
    void saveEvents(String aggregateId, List<StoredEvent> events, int expectedVersion);

    /**
     * Get events for an aggregate
     */
    List<StoredEvent> getEventsForAggregate(String aggregateId);

    /**
     * Get all events since a specific event ID
     */
    List<StoredEvent> getEventsSince(UUID eventId);

    /**
     * Get all events for an aggregate since a specific version
     */
    List<StoredEvent> getEventsForAggregateSinceVersion(String aggregateId, long version);

    /**
     * Get the latest version of an aggregate
     */
    long getLatestVersion(String aggregateId);

    /**
     * Check if an aggregate exists
     */
    boolean aggregateExists(String aggregateId);

    /**
     * Get events by type
     */
    List<StoredEvent> getEventsByType(String eventType);

    /**
     * Get events by correlation ID
     */
    List<StoredEvent> getEventsByCorrelationId(String correlationId);
}
