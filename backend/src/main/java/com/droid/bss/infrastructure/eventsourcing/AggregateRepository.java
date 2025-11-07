package com.droid.bss.infrastructure.eventsourcing;

import org.springframework.stereotype.Repository;

import java.util.function.Function;

/**
 * Base repository for event-sourced aggregates
 */
@Repository
public class AggregateRepository<T extends AggregateRoot> {

    private final EventStore eventStore;
    private final EventPublisher eventPublisher;
    private final Function<String, T> aggregateFactory;

    public AggregateRepository(
            EventStore eventStore,
            EventPublisher eventPublisher,
            Function<String, T> aggregateFactory) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.aggregateFactory = aggregateFactory;
    }

    /**
     * Load aggregate from events
     */
    public T load(String aggregateId, String aggregateType) {
        T aggregate = aggregateFactory.apply(aggregateId);

        // Load events from store
        aggregate.loadFromHistory(eventStore.getEvents(aggregateType, aggregateId));

        return aggregate;
    }

    /**
     * Save aggregate changes
     */
    public void save(T aggregate, String aggregateType) {
        // Get uncommitted changes
        var changes = aggregate.getUncommittedChanges();

        // Publish each change
        for (DomainEvent change : changes) {
            eventPublisher.publish(aggregateType, aggregate.getId(), change);
        }

        // Mark changes as committed
        aggregate.markChangesAsCommitted();
    }
}
