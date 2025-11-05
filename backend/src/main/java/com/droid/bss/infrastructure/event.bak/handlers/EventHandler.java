package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;

/**
 * Interface for handling domain events.
 *
 * Implementations should be stateless and idempotent.
 *
 * @since 1.0
 */
public interface EventHandler<T extends DomainEvent> {

    /**
     * Checks if this handler can handle the given event.
     *
     * @param event the event to check
     * @return true if this handler can handle the event
     */
    boolean canHandle(DomainEvent event);

    /**
     * Handles the given event.
     *
     * @param event the event to handle
     * @return the handling result
     * @throws EventHandlingException if handling fails
     */
    EventHandlingResult handle(T event) throws EventHandlingException;

    /**
     * Gets the event type that this handler handles.
     *
     * @return the event type
     */
    String getSupportedEventType();

    /**
     * Gets the priority of this handler.
     * Higher priority handlers are executed first.
     *
     * @return the priority (default: 0)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Checks if this handler is enabled.
     *
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Gets the name of this handler.
     *
     * @return the handler name
     */
    String getName();
}
