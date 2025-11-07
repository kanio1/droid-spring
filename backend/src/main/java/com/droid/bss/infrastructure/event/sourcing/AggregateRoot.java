package com.droid.bss.infrastructure.event.sourcing;

import io.cloudevents.CloudEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base aggregate root for event sourcing
 */
public abstract class AggregateRoot {

    protected String id;
    protected long version = 0;
    private final List<CloudEvent> domainEvents = new ArrayList<>();

    /**
     * Get the aggregate ID
     */
    public String getId() {
        return id;
    }

    /**
     * Get the current version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Mark aggregate as deleted
     */
    public abstract void markAsDeleted();

    /**
     * Check if aggregate is deleted
     */
    public boolean isDeleted() {
        return false;
    }

    /**
     * Add a domain event
     */
    protected void addDomainEvent(CloudEvent event) {
        domainEvents.add(event);
    }

    /**
     * Get all uncommitted domain events
     */
    public List<CloudEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    /**
     * Clear all uncommitted domain events
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Load from history of events
     */
    public void loadFromHistory(List<StoredEvent> events) {
        events.forEach(event -> {
            applyEvent(event);
            version++;
        });
    }

    /**
     * Apply event to aggregate
     */
    protected abstract void applyEvent(StoredEvent event);
}
