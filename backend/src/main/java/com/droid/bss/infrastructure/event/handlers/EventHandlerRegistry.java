package com.droid.bss.infrastructure.event.handlers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Stub class for event handler registry
 * Minimal implementation for testing purposes
 */
public class EventHandlerRegistry {

    private final Map<Class<?>, List<EventHandler<?>>> handlers;

    public EventHandlerRegistry() {
        this.handlers = new ConcurrentHashMap<>();
    }

    public <T> void registerHandler(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(handler);
    }

    public <T> void unregisterHandler(Class<T> eventType, EventHandler<T> handler) {
        List<EventHandler<?>> list = handlers.get(eventType);
        if (list != null) {
            list.remove(handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<EventHandler<T>> getHandlers(Class<T> eventType) {
        List<EventHandler<?>> rawList = handlers.getOrDefault(eventType, List.of());
        return (List<EventHandler<T>>) (List<?>) rawList;
    }

    public void clear() {
        handlers.clear();
    }
}
