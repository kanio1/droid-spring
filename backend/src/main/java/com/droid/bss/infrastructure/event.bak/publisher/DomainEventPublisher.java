package com.droid.bss.infrastructure.event.publisher;

import java.util.concurrent.CompletableFuture;

/**
 * Port (Interface) for publishing domain events.
 *
 * This interface defines the contract for event publishers,
 * allowing different implementations (Kafka, RabbitMQ, etc.).
 *
 * @since 1.0
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event synchronously.
     *
     * @param event the event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publish(DomainEvent event);

    /**
     * Publishes a domain event asynchronously.
     *
     * @param event the event to publish
     * @return CompletableFuture that completes when the event is published
     */
    CompletableFuture<EventPublishResult> publishAsync(DomainEvent event);

    /**
     * Publishes multiple events in a batch.
     *
     * @param events the events to publish
     * @throws EventPublishingException if publishing fails
     */
    void publishBatch(DomainEvent... events);

    /**
     * Publishes multiple events in a batch asynchronously.
     *
     * @param events the events to publish
     * @return CompletableFuture that completes when all events are published
     */
    CompletableFuture<EventBatchPublishResult> publishBatchAsync(DomainEvent... events);

    /**
     * Flushes any buffered events (for implementations that buffer).
     *
     * @throws EventPublishingException if flush fails
     */
    void flush();

    /**
     * Checks if the publisher is ready to publish events.
     *
     * @return true if ready
     */
    boolean isReady();

    /**
     * Gets the name of the publisher implementation.
     *
     * @return the publisher name
     */
    String getName();
}
