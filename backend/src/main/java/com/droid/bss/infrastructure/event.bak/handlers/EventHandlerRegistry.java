package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for managing event handlers.
 *
 * Provides lookup and execution of handlers for specific event types.
 *
 * @since 1.0
 */
public class EventHandlerRegistry {

    private final Map<String, List<EventHandler<? extends DomainEvent>>> handlersByType = new ConcurrentHashMap<>();
    private final List<EventHandler<? extends DomainEvent>> allHandlers = new CopyOnWriteArrayList<>();

    /**
     * Registers an event handler for a specific event type.
     *
     * @param handler the handler to register
     * @param <T> the event type
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void registerHandler(EventHandler<T> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        String eventType = handler.getSupportedEventType();
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Handler must have a valid event type");
        }

        allHandlers.add(handler);

        handlersByType.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);

        // Sort handlers by priority (higher priority first)
        List<EventHandler<? extends DomainEvent>> handlers = handlersByType.get(eventType);
        handlers.sort(Comparator.comparingInt(EventHandler::getPriority).reversed());

        System.out.println("Registered handler: " + handler.getName() + " for event type: " + eventType);
    }

    /**
     * Unregisters an event handler.
     *
     * @param handler the handler to unregister
     */
    @SuppressWarnings("unchecked")
    public void unregisterHandler(EventHandler<? extends DomainEvent> handler) {
        if (handler == null) {
            return;
        }

        allHandlers.remove(handler);

        String eventType = handler.getSupportedEventType();
        if (eventType != null && !eventType.isBlank()) {
            List<EventHandler<? extends DomainEvent>> handlers = handlersByType.get(eventType);
            if (handlers != null) {
                handlers.remove(handler);
                if (handlers.isEmpty()) {
                    handlersByType.remove(eventType);
                }
            }
        }

        System.out.println("Unregistered handler: " + handler.getName());
    }

    /**
     * Finds handlers that can handle the given event.
     *
     * @param event the event to handle
     * @return the list of handlers (may be empty)
     */
    @SuppressWarnings("unchecked")
    public List<EventHandler<? extends DomainEvent>> findHandlers(DomainEvent event) {
        if (event == null) {
            return Collections.emptyList();
        }

        String eventType = event.getType();
        List<EventHandler<? extends DomainEvent>> handlers = new ArrayList<>();

        // Add handlers for exact event type
        List<EventHandler<? extends DomainEvent>> typeHandlers = handlersByType.get(eventType);
        if (typeHandlers != null) {
            handlers.addAll(typeHandlers);
        }

        // Add generic handlers (handlers that can handle multiple event types)
        for (EventHandler<? extends DomainEvent> handler : allHandlers) {
            if (!handler.getSupportedEventType().equals(eventType)) {
                try {
                    if (handler.canHandle(event)) {
                        handlers.add(handler);
                    }
                } catch (Exception e) {
                    // Log error but continue
                    System.err.println("Error checking if handler can handle event: " + e.getMessage());
                }
            }
        }

        return handlers;
    }

    /**
     * Finds handlers for a specific event type.
     *
     * @param eventType the event type
     * @return the list of handlers (may be empty)
     */
    @SuppressWarnings("unchecked")
    public List<EventHandler<? extends DomainEvent>> findHandlers(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(handlersByType.getOrDefault(eventType, Collections.emptyList()));
    }

    /**
     * Gets all registered handlers.
     *
     * @return the list of all handlers (unmodifiable)
     */
    @SuppressWarnings("unchecked")
    public List<EventHandler<? extends DomainEvent>> getAllHandlers() {
        return Collections.unmodifiableList(new ArrayList<>(allHandlers));
    }

    /**
     * Gets all registered event types.
     *
     * @return the set of event types
     */
    public Set<String> getRegisteredEventTypes() {
        return new HashSet<>(handlersByType.keySet());
    }

    /**
     * Checks if a handler is registered for the given event type.
     *
     * @param eventType the event type
     * @return true if handlers are registered
     */
    public boolean hasHandlers(String eventType) {
        return handlersByType.containsKey(eventType) && !handlersByType.get(eventType).isEmpty();
    }

    /**
     * Checks if there are any handlers registered for any event type.
     *
     * @return true if at least one handler is registered
     */
    public boolean hasAnyHandlers() {
        return !allHandlers.isEmpty();
    }

    /**
     * Gets the number of registered handlers.
     *
     * @return the handler count
     */
    public int getHandlerCount() {
        return allHandlers.size();
    }

    /**
     * Clears all registered handlers.
     */
    public void clear() {
        allHandlers.clear();
        handlersByType.clear();
    }

    /**
     * Gets handler statistics.
     *
     * @return the statistics map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHandlers", allHandlers.size());
        stats.put("eventTypes", handlersByType.size());
        stats.put("handlersByType", new HashMap<>(handlersByType));
        return stats;
    }
}
