package com.droid.bss.infrastructure.event.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test suite for CloudEventHandler
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudEventHandler Unit Tests")
class CloudEventHandlerTest {

    @Mock
    private EventHandlerRegistry registry;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Executor executor;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private TestEventHandler handler;

    private CloudEventHandler cloudEventHandler;

    @BeforeEach
    void setUp() {
        cloudEventHandler = new CloudEventHandler(registry, objectMapper, executor, true);
    }

    @Test
    @DisplayName("Should handle single CloudEvent successfully")
    void shouldHandleSingleCloudEventSuccessfully() {
        // Given
        CloudEvent cloudEvent = createTestCloudEvent("event-1", "customer.created");
        TestDomainEvent domainEvent = new TestDomainEvent("event-1", "customer.created", "source", Instant.now());
        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of(handler));
        given(handler.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler.handle(any(DomainEvent.class))).willReturn(
            EventHandlingResult.success("event-1", "customer.created", "TestHandler", 100L)
        );

        // When
        List<EventHandlingResult> results = cloudEventHandler.handleCloudEvent(cloudEvent, acknowledgment);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isTrue();
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle null CloudEvent")
    void shouldHandleNullCloudEvent() {
        // When
        List<EventHandlingResult> results = cloudEventHandler.handleCloudEvent(null, acknowledgment);

        // Then
        assertThat(results).isEmpty();
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle CloudEvent with no handlers")
    void shouldHandleCloudEventWithNoHandlers() {
        // Given
        CloudEvent cloudEvent = createTestCloudEvent("event-1", "unknown.event");
        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of());

        // When
        List<EventHandlingResult> results = cloudEventHandler.handleCloudEvent(cloudEvent, acknowledgment);

        // Then
        assertThat(results).isEmpty();
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle multiple handlers for single event")
    void shouldHandleMultipleHandlersForSingleEvent() {
        // Given
        CloudEvent cloudEvent = createTestCloudEvent("event-1", "customer.created");
        TestDomainEvent domainEvent = new TestDomainEvent("event-1", "customer.created", "source", Instant.now());
        TestEventHandler handler2 = new TestEventHandler("handler2", "customer.*");
        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of(handler, handler2));
        given(handler.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler2.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler.handle(any(DomainEvent.class))).willReturn(
            EventHandlingResult.success("event-1", "customer.created", "handler1", 100L)
        );
        given(handler2.handle(any(DomainEvent.class))).willReturn(
            EventHandlingResult.success("event-1", "customer.created", "handler2", 150L)
        );

        // When
        List<EventHandlingResult> results = cloudEventHandler.handleCloudEvent(cloudEvent, acknowledgment);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(1).isSuccess()).isTrue();
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle CloudEvent with handler failure")
    void shouldHandleCloudEventWithHandlerFailure() {
        // Given
        CloudEvent cloudEvent = createTestCloudEvent("event-1", "customer.created");
        TestDomainEvent domainEvent = new TestDomainEvent("event-1", "customer.created", "source", Instant.now());
        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of(handler));
        given(handler.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler.handle(any(DomainEvent.class))).willThrow(new RuntimeException("Test error"));

        // When
        List<EventHandlingResult> results = cloudEventHandler.handleCloudEvent(cloudEvent, acknowledgment);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).getMessage()).contains("Test error");
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle batch of CloudEvents")
    void shouldHandleBatchOfCloudEvents() {
        // Given
        CloudEvent event1 = createTestCloudEvent("event-1", "customer.created");
        CloudEvent event2 = createTestCloudEvent("event-2", "order.created");
        List<CloudEvent> events = List.of(event1, event2);

        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of(handler));
        given(handler.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler.handle(any(DomainEvent.class))).willReturn(
            EventHandlingResult.success(anyString(), anyString(), eq("TestHandler"), anyLong())
        );

        // When
        List<List<EventHandlingResult>> allResults = cloudEventHandler.handleCloudEvents(events, acknowledgment);

        // Then
        assertThat(allResults).hasSize(2);
        assertThat(allResults.get(0)).hasSize(1);
        assertThat(allResults.get(1)).hasSize(1);
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should handle empty batch of CloudEvents")
    void shouldHandleEmptyBatchOfCloudEvents() {
        // When
        List<List<EventHandlingResult>> allResults = cloudEventHandler.handleCloudEvents(List.of(), acknowledgment);

        // Then
        assertThat(allResults).isEmpty();
        then(acknowledgment).should().acknowledge();
    }

    @Test
    @DisplayName("Should not acknowledge when autoAcknowledge is false")
    void shouldNotAcknowledgeWhenAutoAcknowledgeIsFalse() {
        // Given
        CloudEventHandler handlerNoAck = new CloudEventHandler(registry, objectMapper, executor, false);
        CloudEvent cloudEvent = createTestCloudEvent("event-1", "customer.created");
        TestDomainEvent domainEvent = new TestDomainEvent("event-1", "customer.created", "source", Instant.now());
        given(registry.findHandlers(any(DomainEvent.class))).willReturn(List.of(handler));
        given(handler.canHandle(any(DomainEvent.class))).willReturn(true);
        given(handler.handle(any(DomainEvent.class))).willReturn(
            EventHandlingResult.success("event-1", "customer.created", "TestHandler", 100L)
        );

        // When
        List<EventHandlingResult> results = handlerNoAck.handleCloudEvent(cloudEvent, acknowledgment);

        // Then
        assertThat(results).hasSize(1);
        then(acknowledgment).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("EventHandlingResult should indicate success correctly")
    void eventHandlingResultShouldIndicateSuccessCorrectly() {
        // When
        EventHandlingResult result = EventHandlingResult.success(
            "event-1", "customer.created", "TestHandler", 100L
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEventId()).isEqualTo("event-1");
        assertThat(result.getEventType()).isEqualTo("customer.created");
        assertThat(result.getHandlerName()).isEqualTo("TestHandler");
        assertThat(result.getProcessingTimeMs()).isEqualTo(100L);
    }

    @Test
    @DisplayName("EventHandlingResult should indicate failure correctly")
    void eventHandlingResultShouldIndicateFailureCorrectly() {
        // When
        EventHandlingResult result = EventHandlingResult.failure(
            "event-1", "customer.created", "TestHandler", "Test error", 200L
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getEventId()).isEqualTo("event-1");
        assertThat(result.getEventType()).isEqualTo("customer.created");
        assertThat(result.getHandlerName()).isEqualTo("TestHandler");
        assertThat(result.getMessage()).isEqualTo("Test error");
        assertThat(result.getProcessingTimeMs()).isEqualTo(200L);
    }

    @Test
    @DisplayName("EventHandlingResult should handle retry counts")
    void eventHandlingResultShouldHandleRetryCounts() {
        // When
        EventHandlingResult result = EventHandlingResult.success(
            "event-1", "customer.created", "TestHandler", 100L, 2
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("EventHandlingException should create specific exceptions")
    void eventHandlingExceptionShouldCreateSpecificExceptions() {
        // Test handler not found
        EventHandlingException handlerNotFound = EventHandlingException.handlerNotFound("unknown.event");
        assertThat(handlerNotFound.getMessage()).contains("No handler found");
        assertThat(handlerNotFound.getErrorCode()).isEqualTo("HANDLER_NOT_FOUND");
        assertThat(handlerNotFound.isRetryable()).isFalse();

        // Test deserialization failure
        EventHandlingException deserializationFailure = EventHandlingException.deserializationFailure(
            "event-1", "customer.created", "JSON error");
        assertThat(deserializationFailure.getMessage()).contains("Failed to deserialize event");
        assertThat(deserializationFailure.getErrorCode()).isEqualTo("DESERIALIZATION_FAILED");

        // Test retryable error
        EventHandlingException retryableError = EventHandlingException.retryableError(
            "event-1", "customer.created", "TestHandler", "Temporary error", new RuntimeException());
        assertThat(retryableError.getMessage()).contains("Retryable error");
        assertThat(retryableError.getErrorCode()).isEqualTo("RETRYABLE_ERROR");
        assertThat(retryableError.isRetryable()).isTrue();
    }

    // Test helper methods
    private CloudEvent createTestCloudEvent(String id, String type) {
        return CloudEventBuilder.v1()
            .withId(id)
            .withType(type)
            .withSource(URI.create("urn:droid:bss:test"))
            .withTime(Instant.now())
            .build();
    }

    // Test helper classes
    private static class TestEventHandler implements EventHandler<DomainEvent> {
        private final String name;
        private final String eventType;
        private boolean enabled = true;

        TestEventHandler(String name, String eventType) {
            this.name = name;
            this.eventType = eventType;
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
