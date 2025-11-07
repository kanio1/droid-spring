package com.droid.bss.infrastructure.event.sourcing;

import java.util.Optional;

/**
 * Store for aggregate snapshots
 */
public interface SnapshotStore {

    /**
     * Save a snapshot
     */
    void saveSnapshot(Snapshot snapshot);

    /**
     * Get latest snapshot for an aggregate
     */
    Optional<Snapshot> getLatestSnapshot(String aggregateId);

    /**
     * Delete snapshot for an aggregate
     */
    void deleteSnapshot(String aggregateId);

    /**
     * Check if aggregate has any snapshots
     */
    boolean hasSnapshot(String aggregateId);
}
