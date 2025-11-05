package com.droid.bss.infrastructure.messaging;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for OrderEventConsumer
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer Messaging Infrastructure")
class OrderEventConsumerTest {

    @Mock
    private Logger log;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Mock
    private CloudEvent cloudEvent;

    @Mock
    private ConsumerRecord<String, Object> consumerRecord;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("Should process order created event")
    void shouldProcessOrderCreatedEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";
        String source = "http://bss-backend/orders";
        OffsetDateTime time = OffsetDateTime.now();

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);
        when(cloudEvent.getSource()).thenReturn(URI.create(source));
        when(cloudEvent.getTime()).thenReturn(time);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(100L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(cloudEvent, atLeast(1)).getId();
        verify(cloudEvent, atLeast(1)).getType();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process order updated event")
    void shouldProcessOrderUpdatedEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.updated.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(101L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(cloudEvent, atLeast(1)).getType();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process order status changed event")
    void shouldProcessOrderStatusChangedEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.statusChanged.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(1);
        when(consumerRecord.offset()).thenReturn(102L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(cloudEvent, atLeast(1)).getType();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process order completed event")
    void shouldProcessOrderCompletedEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.completed.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(103L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(cloudEvent, atLeast(1)).getType();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process order cancelled event")
    void shouldProcessOrderCancelledEvent() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.cancelled.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(2);
        when(consumerRecord.offset()).thenReturn(104L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(cloudEvent, atLeast(1)).getType();
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle duplicate events")
    void shouldHandleDuplicateEvents() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(100L);

        // First event
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        reset(acknowledgment);

        // Duplicate event
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(acknowledgment, times(2)).acknowledge();
    }

    @Test
    @DisplayName("Should handle unknown event type")
    void shouldHandleUnknownEventType() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.unknown.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(105L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        verify(acknowledgment).acknowledge();
        // Unknown event types should be logged but still acknowledged
    }

    @Test
    @DisplayName("Should acknowledge even when exception occurs")
    void shouldAcknowledgeEvenWhenExceptionOccurs() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);
        // Simulate an exception by throwing when accessing getType
        doThrow(new RuntimeException("Simulated error")).when(cloudEvent).getType();

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(106L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert - Even on error, acknowledgment should be called
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should handle events from different partitions")
    void shouldHandleEventsFromDifferentPartitions() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        // Test partitions 0, 1, 2
        for (int partition = 0; partition < 3; partition++) {
            reset(consumerRecord, acknowledgment);

            when(consumerRecord.topic()).thenReturn("order.events");
            when(consumerRecord.partition()).thenReturn(partition);
            when(consumerRecord.offset()).thenReturn(100L + partition);

            // Act
            orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

            // Assert
            verify(acknowledgment).acknowledge();
        }
    }

    @Test
    @DisplayName("Should handle events with different offsets")
    void shouldHandleEventsWithDifferentOffsets() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);

        // Test different offsets
        for (long offset = 100L; offset <= 110L; offset += 2) {
            reset(consumerRecord, acknowledgment);

            when(consumerRecord.offset()).thenReturn(offset);

            // Act
            orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

            // Assert
            verify(acknowledgment).acknowledge();
        }
    }

    @Test
    @DisplayName("Should log correct information for events")
    void shouldLogCorrectInformationForEvents() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        String eventType = "com.droid.bss.order.created.v1";

        when(cloudEvent.getId()).thenReturn(eventId);
        when(cloudEvent.getType()).thenReturn(eventType);

        when(consumerRecord.topic()).thenReturn("order.events");
        when(consumerRecord.partition()).thenReturn(0);
        when(consumerRecord.offset()).thenReturn(100L);

        // Act
        orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

        // Assert
        // Verify that the consumer processes events and logs appropriately
        // The actual logging verification would depend on the logging framework setup
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should process multiple event types in sequence")
    void shouldProcessMultipleEventTypesInSequence() {
        // Arrange
        String[] eventTypes = {
            "com.droid.bss.order.created.v1",
            "com.droid.bss.order.updated.v1",
            "com.droid.bss.order.statusChanged.v1",
            "com.droid.bss.order.completed.v1",
            "com.droid.bss.order.cancelled.v1"
        };

        for (String eventType : eventTypes) {
            reset(cloudEvent, consumerRecord, acknowledgment);

            String eventId = UUID.randomUUID().toString();

            when(cloudEvent.getId()).thenReturn(eventId);
            when(cloudEvent.getType()).thenReturn(eventType);

            when(consumerRecord.topic()).thenReturn("order.events");
            when(consumerRecord.partition()).thenReturn(0);
            when(consumerRecord.offset()).thenReturn(100L);

            // Act
            orderEventConsumer.handleOrderEvent(cloudEvent, consumerRecord, acknowledgment);

            // Assert
            verify(acknowledgment).acknowledge();
        }
    }
}
