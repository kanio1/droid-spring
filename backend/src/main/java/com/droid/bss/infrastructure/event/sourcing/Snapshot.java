package com.droid.bss.infrastructure.event.sourcing;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate snapshot for performance optimization
 */
public class Snapshot {
    private final UUID id;
    private final String aggregateId;
    private final String aggregateType;
    private final String state;
    private final long version;
    private final LocalDateTime createdAt;

    public Snapshot(
            UUID id,
            String aggregateId,
            String aggregateType,
            String state,
            long version,
            LocalDateTime createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.state = state;
        this.version = version;
        this.createdAt = createdAt;
    }

    public static Snapshot create(
            String aggregateId,
            String aggregateType,
            String state,
            long version) {
        return new Snapshot(
                UUID.randomUUID(),
                aggregateId,
                aggregateType,
                state,
                version,
                LocalDateTime.now()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getState() {
        return state;
    }

    public long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
