package com.droid.bss.infrastructure.eventsourcing;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for event-sourced aggregates
 */
public abstract class AggregateRoot {
    protected String id;
    protected int version;
    protected List<DomainEvent> uncommittedChanges;

    public AggregateRoot() {
        this.uncommittedChanges = new ArrayList<>();
        this.version = 0;
    }

    public AggregateRoot(String id) {
        this();
        this.id = id;
    }

    /**
     * Apply event to aggregate
     */
    protected abstract void apply(DomainEvent event);

    /**
     * Load aggregate from history
     */
    public void loadFromHistory(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            apply(event);
            version++;
        }
    }

    /**
     * Raise a new event
     */
    protected void raiseEvent(DomainEvent event) {
        uncommittedChanges.add(event);
        apply(event);
    }

    /**
     * Get uncommitted changes
     */
    public List<DomainEvent> getUncommittedChanges() {
        return new ArrayList<>(uncommittedChanges);
    }

    /**
     * Mark changes as committed
     */
    public void markChangesAsCommitted() {
        uncommittedChanges.clear();
    }

    // Getters
    public String getId() { return id; }
    public int getVersion() { return version; }
    public int getUncommittedChangesCount() { return uncommittedChanges.size(); }
}
