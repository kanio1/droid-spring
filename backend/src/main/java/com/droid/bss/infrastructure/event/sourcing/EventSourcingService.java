package com.droid.bss.infrastructure.event.sourcing;

import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for event sourcing operations
 */
@Service
public class EventSourcingService {

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    private final EventSerializer serializer;

    private static final int SNAPSHOT_INTERVAL = 10;

    public EventSourcingService(
            EventStore eventStore,
            SnapshotStore snapshotStore,
            EventSerializer serializer) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.serializer = serializer;
    }

    /**
     * Save aggregate with events and create snapshot if needed
     */
    public <T extends AggregateRoot> void saveAggregate(T aggregate, String userId) {
        String aggregateId = aggregate.getId();
        String aggregateType = aggregate.getClass().getSimpleName();
        List<CloudEvent> events = aggregate.getDomainEvents();

        if (events.isEmpty()) {
            return;
        }

        // Convert to stored events
        List<StoredEvent> storedEvents = events.stream()
                .map(event -> serializer.fromCloudEvent(
                        event,
                        aggregateId,
                        aggregateType,
                        userId,
                        getCorrelationIdFromEvent(event),
                        aggregate.getVersion() + 1
                ))
                .collect(java.util.stream.Collectors.toList());

        // Save events
        eventStore.saveEvents(aggregateId, storedEvents, (int) aggregate.getVersion());

        // Create snapshot if needed
        long newVersion = aggregate.getVersion() + events.size();
        if (newVersion % SNAPSHOT_INTERVAL == 0) {
            createSnapshot(aggregate, newVersion);
        }

        // Clear uncommitted events
        aggregate.clearDomainEvents();
    }

    /**
     * Load aggregate from event store
     */
    public <T extends AggregateRoot> T loadAggregate(
            String aggregateId,
            Class<T> aggregateType,
            java.util.function.Function<String, T> factory) {

        // Try to load from snapshot first
        Optional<Snapshot> snapshotOpt = snapshotStore.getLatestSnapshot(aggregateId);

        T aggregate;
        long version = 0;

        if (snapshotOpt.isPresent()) {
            Snapshot snapshot = snapshotOpt.get();
            version = snapshot.getVersion();
            // Recreate aggregate from snapshot
            aggregate = recreateFromSnapshot(aggregateId, snapshot, aggregateType, factory);
        } else {
            // Create new aggregate instance
            aggregate = factory.apply(aggregateId);
        }

        // Load events since snapshot
        List<StoredEvent> events = eventStore.getEventsForAggregateSinceVersion(aggregateId, version);
        aggregate.loadFromHistory(events);

        return aggregate;
    }

    /**
     * Replay events for an aggregate
     */
    public void replayEvents(String aggregateId, long fromVersion, long toVersion) {
        // Implementation for event replay
        List<StoredEvent> events = eventStore.getEventsForAggregateSinceVersion(aggregateId, fromVersion);
        // Replay logic would be implemented here
    }

    /**
     * Create snapshot of aggregate
     */
    private <T extends AggregateRoot> void createSnapshot(T aggregate, long version) {
        // Serialize aggregate state
        String state = serializeAggregateState(aggregate);
        Snapshot snapshot = Snapshot.create(
                aggregate.getId(),
                aggregate.getClass().getSimpleName(),
                state,
                version
        );
        snapshotStore.saveSnapshot(snapshot);
    }

    /**
     * Recreate aggregate from snapshot
     */
    private <T extends AggregateRoot> T recreateFromSnapshot(
            String aggregateId,
            Snapshot snapshot,
            Class<T> aggregateType,
            java.util.function.Function<String, T> factory) {

        T aggregate = factory.apply(aggregateId);
        // Deserialize state and apply to aggregate
        deserializeAggregateState(aggregate, snapshot.getState());
        return aggregate;
    }

    /**
     * Serialize aggregate state (to be implemented by subclasses)
     */
    private <T extends AggregateRoot> String serializeAggregateState(T aggregate) {
        // This would be implemented based on aggregate type
        // Could use Jackson to serialize the aggregate state
        throw new UnsupportedOperationException("Aggregate state serialization not implemented");
    }

    /**
     * Deserialize aggregate state (to be implemented by subclasses)
     */
    private <T extends AggregateRoot> void deserializeAggregateState(T aggregate, String state) {
        // This would be implemented based on aggregate type
        // Could use Jackson to deserialize the aggregate state
        throw new UnsupportedOperationException("Aggregate state deserialization not implemented");
    }

    private String getCorrelationIdFromEvent(CloudEvent event) {
        try {
            return serializer.getCorrelationIdFromEvent(event);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
