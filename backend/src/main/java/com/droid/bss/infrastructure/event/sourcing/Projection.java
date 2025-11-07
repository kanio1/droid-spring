package com.droid.bss.infrastructure.event.sourcing;

/**
 * Interface for building projections from events
 */
public interface Projection {

    /**
     * Get the name of the projection
     */
    String getName();

    /**
     * Handle a stored event and update the projection
     */
    void handleEvent(StoredEvent event);

    /**
     * Get the current version of the projection
     */
    long getVersion();

    /**
     * Check if projection is up to date
     */
    boolean isUpToDate();
}
