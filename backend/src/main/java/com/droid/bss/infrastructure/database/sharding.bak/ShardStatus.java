package com.droid.bss.infrastructure.database.sharding;

/**
 * Status of a shard.
 *
 * @since 1.0
 */
public enum ShardStatus {

    /**
     * Shard is active and available for operations.
     */
    ACTIVE("ACTIVE"),

    /**
     * Shard is unavailable (e.g., due to maintenance or failure).
     */
    UNAVAILABLE("UNAVAILABLE"),

    /**
     * Shard is being drained (no new connections, existing operations complete).
     */
    DRAINING("DRAINING"),

    /**
     * Shard is offline.
     */
    OFFLINE("OFFLINE");

    private final String name;

    ShardStatus(String name) {
        this.name = name;
    }

    /**
     * Gets the status name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this status indicates the shard is available.
     *
     * @return true if available
     */
    public boolean isAvailable() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status indicates the shard is unavailable.
     *
     * @return true if unavailable
     */
    public boolean isUnavailable() {
        return this == UNAVAILABLE || this == OFFLINE;
    }

    /**
     * Checks if this status indicates the shard is active (can accept writes).
     *
     * @return true if active
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status indicates the shard is draining.
     *
     * @return true if draining
     */
    public boolean isDraining() {
        return this == DRAINING;
    }

    @Override
    public String toString() {
        return name;
    }
}
