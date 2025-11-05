package com.droid.bss.infrastructure.event.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.mapping.DomainClassMapper;
import org.springframework.kafka.support.mapping.DomainClassMapperHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test suite for KafkaEventPublisher
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher Unit Tests")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventProperties eventProperties;

    @Mock
    private SendResult<String, Object> sendResult;

    @Mock
    private org.apache.kafka.common.record.RecordMetadata recordMetadata;

    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        // Configure default properties
        given(eventProperties.getEnabled()).willReturn(true);
        given(eventProperties.getTopicPrefix()).willReturn("bss.events");
        given(eventProperties.getAsyncTimeout()).willReturn(Duration.ofSeconds(30));
        given(eventProperties.getParallelism()).willReturn(5);
        given(eventProperties.getBufferSize()).willReturn(1000);

        publisher = new KafkaEventPublisher(kafkaTemplate, objectMapper, eventProperties);
    }

    @Test
    @DisplayName("Should create publisher with default configuration")
    void shouldCreatePublisherWithDefaultConfiguration() {
        // When
        KafkaEventPublisher newPublisher = new KafkaEventPublisher();

        // Then
        assertThat(newPublisher).isNotNull();
        assertThat(newPublisher.getName()).isEqualTo("KafkaEventPublisher");
    }

    @Test
    @DisplayName("Should publish single event successfully")
    void shouldPublishSingleEventSuccessfully() throws Exception {
        // Given
        TestDomainEvent event = new TestDomainEvent("test-event-123", "test.event",
            "urn:droid:bss:test", Instant.now(), null, "test-subject", "partition-key-1");
        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(CompletableFuture.completedFuture(sendResult));
        given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
        given(recordMetadata.partition()).willReturn(1);
        given(recordMetadata.offset()).willReturn(100L);

        // When
        publisher.publish(event);

        // Then
        then(kafkaTemplate).should().send(eq("bss.events.test-event"), eq("partition-key-1"), any(CloudEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when publishing null event")
    void shouldThrowExceptionWhenPublishingNullEvent() {
        // When & Then
        assertThatThrownBy(() -> publisher.publish(null))
            .isInstanceOf(EventPublishingException.class)
            .hasMessageContaining("Event cannot be null");
    }

    @Test
    @DisplayName("Should publish event when publishing is disabled")
    void shouldSkipPublishingWhenDisabled() {
        // Given
        given(eventProperties.getEnabled()).willReturn(false);
        TestDomainEvent event = new TestDomainEvent("test-event", "test.event",
            "urn:droid:bss:test", Instant.now(), null, null, null);

        // When
        publisher.publish(event);

        // Then
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should publish event asynchronously successfully")
    void shouldPublishEventAsynchronouslySuccessfully() throws Exception {
        // Given
        TestDomainEvent event = new TestDomainEvent("async-event", "test.async",
            "urn:droid:bss:test", Instant.now(), null, "async-subject", "async-key");
        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(CompletableFuture.completedFuture(sendResult));
        given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
        given(recordMetadata.partition()).willReturn(2);
        given(recordMetadata.offset()).willReturn(200L);

        // When
        CompletableFuture<EventPublishResult> future = publisher.publishAsync(event);
        EventPublishResult result = future.get();

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEventId()).isEqualTo("async-event");
        assertThat(result.getTopic()).isEqualTo("bss.events.test-async");
        assertThat(result.getPartition()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(200L);
        assertThat(result.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle async publishing failure")
    void shouldHandleAsyncPublishingFailure() throws Exception {
        // Given
        TestDomainEvent event = new TestDomainEvent("failing-event", "test.failing",
            "urn:droid:bss:test", Instant.now(), null, null, null);
        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(CompletableFuture.failedFuture(new RuntimeException("Test error")));

        // When
        CompletableFuture<EventPublishResult> future = publisher.publishAsync(event);
        EventPublishResult result = future.get();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getEventId()).isEqualTo("failing-event");
        assertThat(result.getErrorMessage()).contains("Test error");
    }

    @Test
    @DisplayName("Should handle null event in async publishing")
    void shouldHandleNullEventInAsyncPublishing() throws Exception {
        // When
        CompletableFuture<EventPublishResult> future = publisher.publishAsync(null);
        EventPublishResult result = future.get();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Event cannot be null");
    }

    @Test
    @DisplayName("Should publish batch of events successfully")
    void shouldPublishBatchOfEventsSuccessfully() throws Exception {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("batch-1", "test.batch",
            "urn:droid:bss:test", Instant.now(), null, "subject-1", "key-1");
        TestDomainEvent event2 = new TestDomainEvent("batch-2", "test.batch",
            "urn:droid:bss:test", Instant.now(), null, "subject-2", "key-2");
        TestDomainEvent event3 = new TestDomainEvent("batch-3", "test.batch",
            "urn:droid:bss:test", Instant.now(), null, "subject-3", "key-3");

        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(CompletableFuture.completedFuture(sendResult));
        given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
        given(recordMetadata.partition()).willReturn(1);

        // When
        publisher.publishBatch(event1, event2, event3);

        // Then
        then(kafkaTemplate).should(times(3)).send(anyString(), anyString(), any(CloudEvent.class));
    }

    @Test
    @DisplayName("Should handle empty batch")
    void shouldHandleEmptyBatch() {
        // When
        publisher.publishBatch();

        // Then
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should handle null batch")
    void shouldHandleNullBatch() {
        // When
        publisher.publishBatch((TestDomainEvent[]) null);

        // Then
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should publish batch of events asynchronously")
    void shouldPublishBatchOfEventsAsynchronously() throws Exception {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("async-batch-1", "test.asyncBatch",
            "urn:droid:bss:test", Instant.now(), null, "subject-1", "key-1");
        TestDomainEvent event2 = new TestDomainEvent("async-batch-2", "test.asyncBatch",
            "urn:droid:bss:test", Instant.now(), null, "subject-2", "key-2");

        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(CompletableFuture.completedFuture(sendResult));
        given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
        given(recordMetadata.partition()).willReturn(1);
        given(recordMetadata.offset()).willReturn(50L);

        // When
        CompletableFuture<EventBatchPublishResult> future = publisher.publishBatchAsync(event1, event2);
        EventBatchPublishResult result = future.get();

        // Then
        assertThat(result.getTotalEvents()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailureCount()).isEqualTo(0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResults()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle async batch with mixed results")
    void shouldHandleAsyncBatchWithMixedResults() throws Exception {
        // Given
        TestDomainEvent event1 = new TestDomainEvent("mixed-1", "test.mixed",
            "urn:droid:bss:test", Instant.now(), null, "subject-1", "key-1");
        TestDomainEvent event2 = new TestDomainEvent("mixed-2", "test.mixed",
            "urn:droid:bss:test", Instant.now(), null, "subject-2", "key-2");

        given(kafkaTemplate.send(anyString(), anyString(), any(CloudEvent.class)))
            .willReturn(
                CompletableFuture.completedFuture(sendResult),
                CompletableFuture.failedFuture(new RuntimeException("Publish failed"))
            );
        given(sendResult.getRecordMetadata()).willReturn(recordMetadata);
        given(recordMetadata.partition()).willReturn(1);

        // When
        CompletableFuture<EventBatchPublishResult> future = publisher.publishBatchAsync(event1, event2);
        EventBatchPublishResult result = future.get();

        // Then
        assertThat(result.getTotalEvents()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedResults()).hasSize(1);
        assertThat(result.getSuccessResults()).hasSize(1);
    }

    @Test
    @DisplayName("Should flush events")
    void shouldFlushEvents() {
        // When
        publisher.flush();

        // Then
        then(kafkaTemplate).should().flush();
    }

    @Test
    @DisplayName("Should return ready status when publisher is available")
    void shouldReturnReadyStatusWhenPublisherIsAvailable() {
        // When
        boolean ready = publisher.isReady();

        // Then
        assertThat(ready).isTrue();
    }

    @Test
    @DisplayName("Should return not ready when publishing is disabled")
    void shouldReturnNotReadyWhenPublishingIsDisabled() {
        // Given
        given(eventProperties.getEnabled()).willReturn(false);

        // When
        boolean ready = publisher.isReady();

        // Then
        assertThat(ready).isFalse();
    }

    @Test
    @DisplayName("Should return publisher name")
    void shouldReturnPublisherName() {
        // When
        String name = publisher.getName();

        // Then
        assertThat(name).isEqualTo("KafkaEventPublisher");
    }

    @Test
    @DisplayName("EventPublishResult should indicate success correctly")
    void eventPublishResultShouldIndicateSuccessCorrectly() {
        // When
        EventPublishResult result = EventPublishResult.success(
            "event-123",
            "bss.events.test",
            Instant.now(),
            "partition-key",
            1,
            100L
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getEventId()).isEqualTo("event-123");
        assertThat(result.getTopic()).isEqualTo("bss.events.test");
        assertThat(result.getPartition()).isEqualTo(1);
        assertThat(result.getOffset()).isEqualTo(100L);
        assertThat(result.getPartitionKey()).isEqualTo("partition-key");
    }

    @Test
    @DisplayName("EventPublishResult should indicate failure correctly")
    void eventPublishResultShouldIndicateFailureCorrectly() {
        // When
        EventPublishResult result = EventPublishResult.failure(
            "event-456",
            "bss.events.test",
            "Test error message"
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getEventId()).isEqualTo("event-456");
        assertThat(result.getTopic()).isEqualTo("bss.events.test");
        assertThat(result.getErrorMessage()).isEqualTo("Test error message");
    }

    @Test
    @DisplayName("EventBatchPublishResult should handle successful batch")
    void eventBatchPublishResultShouldHandleSuccessfulBatch() {
        // Given
        EventPublishResult result1 = EventPublishResult.success("event-1", "topic1", Instant.now(),
            "key1", 1, 100L);
        EventPublishResult result2 = EventPublishResult.success("event-2", "topic1", Instant.now(),
            "key2", 1, 101L);
        List<EventPublishResult> results = List.of(result1, result2);

        // When
        EventBatchPublishResult batchResult = EventBatchPublishResult.success(results, Duration.ofMillis(100));

        // Then
        assertThat(batchResult.isSuccess()).isTrue();
        assertThat(batchResult.getTotalEvents()).isEqualTo(2);
        assertThat(batchResult.getSuccessCount()).isEqualTo(2);
        assertThat(batchResult.getFailureCount()).isEqualTo(0);
        assertThat(batchResult.getResults()).hasSize(2);
        assertThat(batchResult.getFailedResults()).isEmpty();
        assertThat(batchResult.getSuccessResults()).hasSize(2);
    }

    @Test
    @DisplayName("EventBatchPublishResult should handle failed batch")
    void eventBatchPublishResultShouldHandleFailedBatch() {
        // Given
        EventPublishResult result1 = EventPublishResult.success("event-1", "topic1", Instant.now(),
            "key1", 1, 100L);
        EventPublishResult result2 = EventPublishResult.failure("event-2", "topic1", "Error");
        List<EventPublishResult> results = List.of(result1, result2);

        // When
        EventBatchPublishResult batchResult = EventBatchPublishResult.failure(2, results, Duration.ofMillis(100));

        // Then
        assertThat(batchResult.isSuccess()).isFalse();
        assertThat(batchResult.getTotalEvents()).isEqualTo(2);
        assertThat(batchResult.getSuccessCount()).isEqualTo(1);
        assertThat(batchResult.getFailureCount()).isEqualTo(1);
        assertThat(batchResult.getFailedResults()).hasSize(1);
        assertThat(batchResult.getSuccessResults()).hasSize(1);
    }

    @Test
    @DisplayName("EventPublishingException should create specific exceptions")
    void eventPublishingExceptionShouldCreateSpecificExceptions() {
        // Test serialization failure
        EventPublishingException serializationEx = EventPublishingException.serializationFailure(
            "event-1", "test.event", "topic", "JSON error");
        assertThat(serializationEx.getMessage()).contains("Failed to serialize event");
        assertThat(serializationEx.getErrorCode()).isEqualTo("SERIALIZATION_ERROR");

        // Test topic not found
        EventPublishingException topicEx = EventPublishingException.topicNotFound("missing-topic");
        assertThat(topicEx.getMessage()).contains("Topic not found");
        assertThat(topicEx.getErrorCode()).isEqualTo("TOPIC_NOT_FOUND");

        // Test invalid event
        EventPublishingException invalidEx = EventPublishingException.invalidEvent("Event is null");
        assertThat(invalidEx.getMessage()).contains("Invalid event");
        assertThat(invalidEx.getErrorCode()).isEqualTo("INVALID_EVENT");
    }

    // Test helper class
    private static class TestDomainEvent implements DomainEvent {
        private final String id;
        private final String type;
        private final String source;
        private final Instant time;
        private final String schemaUrl;
        private final String subject;
        private final String partitionKey;

        TestDomainEvent(String id, String type, String source, Instant time,
                       String schemaUrl, String subject, String partitionKey) {
            this.id = id;
            this.type = type;
            this.source = source;
            this.time = time;
            this.schemaUrl = schemaUrl;
            this.subject = subject;
            this.partitionKey = partitionKey;
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
            return schemaUrl;
        }

        @Override
        public String getSubject() {
            return subject;
        }

        @Override
        public String getPartitionKey() {
            return partitionKey;
        }
    }
}
