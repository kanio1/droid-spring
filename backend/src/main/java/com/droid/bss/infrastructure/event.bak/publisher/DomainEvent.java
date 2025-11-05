package com.droid.bss.infrastructure.event.publisher;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for all domain events.
 *
 * Domain events represent something that happened in the domain that
 * other parts of the system might be interested in.
 *
 * Implementations should follow the CloudEvents v1.0 specification
 * for interoperability.
 *
 * @since 1.0
 */
public interface DomainEvent {

    /**
     * Gets the unique identifier of the event.
     *
     * @return the event ID (UUID)
     */
    String getId();

    /**
     * Gets the type of the event.
     * Should follow the naming convention: domain.entity.action
     * Example: customer.created, order.updated, payment.processed
     *
     * @return the event type
     */
    String getType();

    /**
     * Gets the source of the event.
     * Should be a URN pointing to the service/application.
     * Example: urn:droid:bss:customer-service
     *
     * @return the event source
     */
    String getSource();

    /**
     * Gets the time when the event occurred.
     *
     * @return the event timestamp
     */
    Instant getTime();

    /**
     * Gets the data schema URL (optional).
     *
     * @return the schema URL (may be null)
     */
    String getSchemaUrl();

    /**
     * Gets the subject of the event (optional).
     * Usually the ID of the entity that the event is about.
     *
     * @return the subject (may be null)
     */
    String getSubject();

    /**
     * Gets the partition key for event routing (optional).
     * Used for Kafka partitioning to ensure events with the same
     * key go to the same partition.
     *
     * @return the partition key (may be null)
     */
    String getPartitionKey();
}
