package com.droid.bss.infrastructure.event.sourcing.impl;

import com.droid.bss.infrastructure.event.sourcing.Snapshot;
import com.droid.bss.infrastructure.event.sourcing.SnapshotStore;
import com.droid.bss.infrastructure.event.sourcing.entity.SnapshotEntity;
import com.droid.bss.infrastructure.event.sourcing.repository.SnapshotEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of SnapshotStore using JPA
 */
@Service
public class SnapshotStoreImpl implements SnapshotStore {

    private final SnapshotEntityRepository repository;

    public SnapshotStoreImpl(SnapshotEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveSnapshot(Snapshot snapshot) {
        // Delete existing snapshot for this aggregate
        repository.deleteByAggregateId(snapshot.getAggregateId());

        // Save new snapshot
        SnapshotEntity entity = SnapshotEntity.fromSnapshot(snapshot);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> getLatestSnapshot(String aggregateId) {
        return repository.findByAggregateId(aggregateId)
                .map(this::toSnapshot);
    }

    @Override
    @Transactional
    public void deleteSnapshot(String aggregateId) {
        repository.deleteByAggregateId(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSnapshot(String aggregateId) {
        return repository.existsByAggregateId(aggregateId);
    }

    private Snapshot toSnapshot(SnapshotEntity entity) {
        return new Snapshot(
                entity.getId(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getState(),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }
}
