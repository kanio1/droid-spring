package com.droid.bss.infrastructure.event.sourcing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for storing aggregate snapshots
 */
@Entity
@Table(name = "snapshot_store")
public class SnapshotEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, unique = true)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "state", nullable = false, columnDefinition = "LONGTEXT")
    private String state;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SnapshotEntity() {
    }

    public SnapshotEntity(
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

    public static SnapshotEntity fromSnapshot(
            com.droid.bss.infrastructure.event.sourcing.Snapshot snapshot) {
        return new SnapshotEntity(
                snapshot.getId(),
                snapshot.getAggregateId(),
                snapshot.getAggregateType(),
                snapshot.getState(),
                snapshot.getVersion(),
                snapshot.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnapshotEntity that = (SnapshotEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
