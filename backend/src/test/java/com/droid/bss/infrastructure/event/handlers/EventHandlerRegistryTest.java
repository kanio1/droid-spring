package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for EventHandlerRegistry
 *
 * @since 1.0
 */
@DisplayName("EventHandlerRegistry Unit Tests")
class EventHandlerRegistryTest {

    private EventHandlerRegistry registry;
    private TestEventHandler handler1;
    private TestEventHandler handler2;
    private TestEventHandler handler3;

    @BeforeEach
    void setUp() {
        registry = new EventHandlerRegistry();
        handler1 = new TestEventHandler("handler1", "customer.created", 10);
        handler2 = new TestEventHandler("handler2", "customer.updated", 5);
        handler3 = new TestEventHandler("handler3", "order.created", 8);
    }

    @Test
    @DisplayName("Should register single handler")
    void shouldRegisterSingleHandler() {
        // When
        registry.registerHandler(handler1);

        // Then
        assertThat(registry.getHandlerCount()).isEqualTo(1);
        assertThat(registry.hasHandlers("customer.created")).isTrue();
    }

    @Test
    @DisplayName("Should register multiple handlers")
    void shouldRegisterMultipleHandlers() {
        // When
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);
        registry.registerHandler(handler3);

        // Then
        assertThat(registry.getHandlerCount()).isEqualTo(3);
        assertThat(registry.getRegisteredEventTypes()).hasSize(2);
    }

    @Test
    @DisplayName("Should sort handlers by priority")
    void shouldSortHandlersByPriority() {
        // Given
        TestEventHandler highPriority = new TestEventHandler("high", "customer.created", 20);
        TestEventHandler lowPriority = new TestEventHandler("low", "customer.created", 1);

        // When
        registry.registerHandler(lowPriority);
        registry.registerHandler(handler1);
        registry.registerHandler(highPriority);

        // Then
        List<EventHandler<? extends DomainEvent>> handlers = registry.findHandlers("customer.created");
        assertThat(handlers).hasSize(3);
        assertThat(handlers.get(0).getName()).isEqualTo("high");
        assertThat(handlers.get(1).getName()).isEqualTo("handler1");
        assertThat(handlers.get(2).getName()).isEqualTo("low");
    }

    @Test
    @DisplayName("Should unregister handler")
    void shouldUnregisterHandler() {
        // Given
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);

        // When
        registry.unregisterHandler(handler1);

        // Then
        assertThat(registry.getHandlerCount()).isEqualTo(1);
        assertThat(registry.findHandlers("customer.created")).hasSize(1);
    }

    @Test
    @DisplayName("Should find handlers for specific event type")
    void shouldFindHandlersForSpecificEventType() {
        // Given
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);
        registry.registerHandler(handler3);

        // When
        List<EventHandler<? extends DomainEvent>> handlers = registry.findHandlers("customer.created");

        // Then
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isEqualTo(handler1);
    }

    @Test
    @DisplayName("Should find handlers for event")
    void shouldFindHandlersForEvent() {
        // Given
        TestDomainEvent event = new TestDomainEvent("event-1", "customer.created", "source", Instant.now());
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);
        registry.registerHandler(handler3);

        // When
        List<EventHandler<? extends DomainEvent>> handlers = registry.findHandlers(event);

        // Then
        assertThat(handlers).hasSize(1);
        assertThat(handlers.get(0)).isEqualTo(handler1);
    }

    @Test
    @DisplayName("Should return empty list when no handlers found")
    void shouldReturnEmptyListWhenNoHandlersFound() {
        // Given
        registry.registerHandler(handler1);

        // When
        List<EventHandler<? extends DomainEvent>> handlers = registry.findHandlers("unknown.event");

        // Then
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Should handle null event")
    void shouldHandleNullEvent() {
        // When
        List<EventHandler<? extends DomainEvent>> handlers = registry.findHandlers((DomainEvent) null);

        // Then
        assertThat(handlers).isEmpty();
    }

    @Test
    @DisplayName("Should get all registered handlers")
    void shouldGetAllRegisteredHandlers() {
        // Given
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);

        // When
        List<EventHandler<? extends DomainEvent>> allHandlers = registry.getAllHandlers();

        // Then
        assertThat(allHandlers).hasSize(2);
        assertThat(allHandlers).containsExactlyInAnyOrder(handler1, handler2);
    }

    @Test
    @DisplayName("Should check if handlers exist")
    void shouldCheckIfHandlersExist() {
        // Given
        registry.registerHandler(handler1);

        // Then
        assertThat(registry.hasHandlers("customer.created")).isTrue();
        assertThat(registry.hasHandlers("unknown.event")).isFalse();
    }

    @Test
    @DisplayName("Should clear all handlers")
    void shouldClearAllHandlers() {
        // Given
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);

        // When
        registry.clear();

        // Then
        assertThat(registry.getHandlerCount()).isEqualTo(0);
        assertThat(registry.getRegisteredEventTypes()).isEmpty();
    }

    @Test
    @DisplayName("Should get statistics")
    void shouldGetStatistics() {
        // Given
        registry.registerHandler(handler1);
        registry.registerHandler(handler2);

        // When
        var stats = registry.getStatistics();

        // Then
        assertThat(stats).containsKey("totalHandlers");
        assertThat(stats).containsKey("eventTypes");
        assertThat(stats.get("totalHandlers")).isEqualTo(2);
        assertThat(stats.get("eventTypes")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw exception when registering null handler")
    void shouldThrowExceptionWhenRegisteringNullHandler() {
        // When & Then
        assertThatThrownBy(() -> registry.registerHandler(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Handler cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when handler has invalid event type")
    void shouldThrowExceptionWhenHandlerHasInvalidEventType() {
        // Given
        TestEventHandler invalidHandler = new TestEventHandler("invalid", "", 1);

        // When & Then
        assertThatThrownBy(() -> registry.registerHandler(invalidHandler))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Handler must have a valid event type");
    }

    // Test helper classes
    private static class TestEventHandler implements EventHandler<DomainEvent> {
        private final String name;
        private final String eventType;
        private final int priority;
        private boolean enabled = true;

        TestEventHandler(String name, String eventType, int priority) {
            this.name = name;
            this.eventType = eventType;
            this.priority = priority;
        }

        @Override
        public boolean canHandle(DomainEvent event) {
            return event != null && event.getType().equals(eventType);
        }

        @Override
        public EventHandlingResult handle(DomainEvent event) throws EventHandlingException {
            return EventHandlingResult.success(event.getId(), event.getType(), name, 0);
        }

        @Override
        public String getSupportedEventType() {
            return eventType;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static class TestDomainEvent implements DomainEvent {
        private final String id;
        private final String type;
        private final String source;
        private final Instant time;

        TestDomainEvent(String id, String type, String source, Instant time) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.time = time;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public Instant getTime() {
            return time;
        }

        @Override
        public String getSchemaUrl() {
            return null;
        }

        @Override
        public String getSubject() {
            return null;
        }

        @Override
        public String getPartitionKey() {
            return null;
        }
    }
}
