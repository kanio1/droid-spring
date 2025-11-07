package com.droid.bss.domain.outbox;

/**
 * Outbox Event Status
 *
 * Tracks the lifecycle of outbox events
 */
public enum OutboxStatus {

    /**
     * Event created, waiting to be published
     */
    PENDING("Pending"),

    /**
     * Event successfully published
     */
    PUBLISHED("Published"),

    /**
     * Event failed to publish, will be retried
     */
    RETRY("Retry"),

    /**
     * Event failed to publish after max retries, needs manual intervention
     */
    DEAD_LETTER("Dead Letter");

    private final String displayName;

    OutboxStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if event is in a final state
     */
    public boolean isFinalState() {
        return this == PUBLISHED || this == DEAD_LETTER;
    }

    /**
     * Check if event is pending publication
     */
    public boolean isPending() {
        return this == PENDING;
    }
}
