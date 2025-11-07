package com.droid.bss.infrastructure.event.handlers;

/**
 * Stub interface for event handlers
 * Minimal implementation for testing purposes
 */
public interface EventHandler<T> {

    /**
     * Handle an event
     */
    void handle(T event);

    /**
     * Get the event type this handler supports
     */
    Class<T> getEventType();
}
