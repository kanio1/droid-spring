package com.droid.bss.infrastructure.event.sourcing.impl;

import com.droid.bss.infrastructure.event.sourcing.EventStore;
import com.droid.bss.infrastructure.event.sourcing.StoredEvent;
import com.droid.bss.infrastructure.event.sourcing.entity.EventEntity;
import com.droid.bss.infrastructure.event.sourcing.repository.EventEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of EventStore using JPA
 */
@Service
public class EventStoreImpl implements EventStore {

    private final EventEntityRepository repository;

    public EventStoreImpl(EventEntityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveEvents(String aggregateId, List<StoredEvent> events, int expectedVersion) {
        // Verify expected version
        long currentVersion = repository.getLatestVersion(aggregateId);
        if (currentVersion != expectedVersion) {
            throw new RuntimeException(
                    String.format("Optimistic locking conflict. Expected version: %d, Current version: %d",
                            expectedVersion, currentVersion)
            );
        }

        // Save events
        List<EventEntity> eventEntities = events.stream()
                .map(EventEntity::fromStoredEvent)
                .collect(Collectors.toList());

        repository.saveAll(eventEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsForAggregate(String aggregateId) {
        return repository.findByAggregateIdOrderByVersion(aggregateId)
                .stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsSince(UUID eventId) {
        return repository.findEventsAfterId(eventId)
                .stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsForAggregateSinceVersion(String aggregateId, long version) {
        return repository.findByAggregateIdAndVersionGreaterThan(aggregateId, version)
                .stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getLatestVersion(String aggregateId) {
        return repository.getLatestVersion(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean aggregateExists(String aggregateId) {
        return repository.existsByAggregateId(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsByType(String eventType) {
        return repository.findByEventType(eventType)
                .stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredEvent> getEventsByCorrelationId(String correlationId) {
        return repository.findByCorrelationId(correlationId)
                .stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    private StoredEvent toStoredEvent(EventEntity entity) {
        return new StoredEvent(
                entity.getId(),
                entity.getAggregateId(),
                entity.getAggregateType(),
                entity.getEventType(),
                entity.getEventData(),
                entity.getTimestamp(),
                entity.getUserId(),
                entity.getCorrelationId(),
                entity.getVersion()
        );
    }
}
