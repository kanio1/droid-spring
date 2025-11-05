package com.droid.bss.infrastructure.messaging.deadletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Test suite for KafkaDeadLetterQueue
 *
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaDeadLetterQueue Unit Tests")
class KafkaDeadLetterQueueTest {

    @Mock
    private DeadLetterQueueConfig config;

    @Mock
    private org.apache.kafka.clients.producer.KafkaProducer<String, String> producer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProducerRecord<String, String> producerRecord;

    @Mock
    private RecordMetadata recordMetadata;

    private KafkaDeadLetterQueue dlq;

    @BeforeEach
    void setUp() {
        // Configure default config
        given(config.getName()).willReturn("test-dlq");
        given(config.getTopicPrefix()).willReturn("dlq");
        given(config.getMaxMessages()).willReturn(100000L);
        given(config.getMaxPayloadSize()).willReturn(1024000L);
        given(config.getStorePayload()).willReturn(true);
        given(config.getStoreStackTraces()).willReturn(true);
        given(config.getRetentionTime()).willReturn(java.time.Duration.ofDays(7));

        dlq = new KafkaDeadLetterQueue(config, producer, objectMapper);
    }

    @Test
    @DisplayName("Should create DLQ with configuration")
    void shouldCreateDLQWithConfiguration() {
        // Then
        assertThat(dlq).isNotNull();
        assertThat(dlq.getName()).isEqualTo("test-dlq");
    }

    @Test
    @DisplayName("Should send message to DLQ")
    void shouldSendMessageToDLQ() throws DeadLetterQueueException {
        // Given
        DLQEntry entry = createTestEntry("msg-1", "test.topic", "Test error", "ERROR_TYPE");
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        // When
        dlq.send(entry);

        // Then
        then(producer).should().send(any(ProducerRecord.class));
        assertThat(dlq.getStats().getTotalAdded()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should send message to DLQ asynchronously")
    void shouldSendMessageToDLQAsynchronously() throws Exception {
        // Given
        DLQEntry entry = createTestEntry("async-msg", "async.topic", "Async error", "ASYNC_ERROR");
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        // When
        CompletableFuture<Void> future = dlq.sendAsync(entry);
        future.get();

        // Then
        then(producer).should().send(any(ProducerRecord.class));
        assertThat(dlq.getStats().getTotalAdded()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should send batch of messages to DLQ")
    void shouldSendBatchOfMessagesToDLQ() throws DeadLetterQueueException {
        // Given
        DLQEntry entry1 = createTestEntry("batch-1", "batch.topic", "Batch error 1", "BATCH_ERROR");
        DLQEntry entry2 = createTestEntry("batch-2", "batch.topic", "Batch error 2", "BATCH_ERROR");
        List<DLQEntry> entries = List.of(entry1, entry2);
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        // When
        dlq.sendBatch(entries);

        // Then
        then(producer).should(times(2)).send(any(ProducerRecord.class));
        assertThat(dlq.getStats().getTotalAdded()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should send batch of messages to DLQ asynchronously")
    void shouldSendBatchOfMessagesToDLQAsynchronously() throws Exception {
        // Given
        DLQEntry entry1 = createTestEntry("async-batch-1", "async.topic", "Async batch error 1", "ASYNC_BATCH_ERROR");
        DLQEntry entry2 = createTestEntry("async-batch-2", "async.topic", "Async batch error 2", "ASYNC_BATCH_ERROR");
        List<DLQEntry> entries = List.of(entry1, entry2);
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        // When
        CompletableFuture<Void> future = dlq.sendBatchAsync(entries);
        future.get();

        // Then
        then(producer).should(times(2)).send(any(ProducerRecord.class));
        assertThat(dlq.getStats().getTotalAdded()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception for null entry")
    void shouldThrowExceptionForNullEntry() {
        // When & Then
        assertThatThrownBy(() -> dlq.send(null))
            .isInstanceOf(DeadLetterQueueException.class)
            .hasMessageContaining("DLQ entry cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for invalid entry")
    void shouldThrowExceptionForInvalidEntry() {
        // Given
        DLQEntry entry = DLQEntry.newBuilder()
            .errorMessage("Error")
            .errorType("ERROR_TYPE")
            .build();

        // When & Then
        assertThatThrownBy(() -> dlq.send(entry))
            .isInstanceOf(DeadLetterQueueException.class)
            .hasMessageContaining("Topic cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when DLQ is closed")
    void shouldThrowExceptionWhenDLQIsClosed() throws Exception {
        // Given
        dlq.close();

        // When & Then
        assertThatThrownBy(() -> dlq.send(createTestEntry("msg", "topic", "error", "ERROR")))
            .isInstanceOf(DeadLetterQueueException.class)
            .hasMessageContaining("DLQ is closed");
    }

    @Test
    @DisplayName("Should check if DLQ is healthy")
    void shouldCheckIfDLQIsHealthy() {
        // Given
        given(producer.metrics()).willReturn(Map.of());

        // When
        boolean healthy = dlq.isHealthy();

        // Then
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("Should check if DLQ is not healthy when closed")
    void shouldCheckIfDLQIsNotHealthyWhenClosed() throws Exception {
        // Given
        dlq.close();

        // When
        boolean healthy = dlq.isHealthy();

        // Then
        assertThat(healthy).isFalse();
    }

    @Test
    @DisplayName("Should get DLQ statistics")
    void shouldGetDLQStats() throws DeadLetterQueueException {
        // Given
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        DLQEntry entry1 = createTestEntry("stats-1", "stats.topic", "Stats error 1", "STATS_ERROR");
        DLQEntry entry2 = createTestEntry("stats-2", "stats.topic", "Stats error 2", "STATS_ERROR");

        // When
        dlq.send(entry1);
        dlq.send(entry2);

        // Then
        DLQStats stats = dlq.getStats();
        assertThat(stats.getTotalAdded()).isEqualTo(2);
        assertThat(stats.getTotalMessages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should close DLQ")
    void shouldCloseDLQ() throws DeadLetterQueueException {
        // When
        dlq.close();

        // Then
        // Verify no exception is thrown
        assertThat(dlq.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should record requeue statistics")
    void shouldRecordRequeueStatistics() {
        // Given
        DLQStats stats = dlq.getStats();

        // When
        stats.recordRequeue("test.topic");

        // Then
        assertThat(stats.getTotalRequeued()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should record error statistics")
    void shouldRecordErrorStatistics() {
        // Given
        DLQStats stats = dlq.getStats();

        // When
        stats.recordError();

        // Then
        assertThat(stats.getTotalErrors()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track messages by topic")
    void shouldTrackMessagesByTopic() throws DeadLetterQueueException {
        // Given
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        DLQEntry entry1 = createTestEntry("topic-1", "customer.topic", "Error 1", "CUSTOMER_ERROR");
        DLQEntry entry2 = createTestEntry("topic-2", "order.topic", "Error 2", "ORDER_ERROR");
        DLQEntry entry3 = createTestEntry("topic-3", "customer.topic", "Error 3", "CUSTOMER_ERROR");

        // When
        dlq.send(entry1);
        dlq.send(entry2);
        dlq.send(entry3);

        // Then
        DLQStats stats = dlq.getStats();
        Map<String, Long> byTopic = stats.getMessagesByTopic();
        assertThat(byTopic.get("customer.topic")).isEqualTo(2);
        assertThat(byTopic.get("order.topic")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track messages by error type")
    void shouldTrackMessagesByErrorType() throws DeadLetterQueueException {
        // Given
        given(producer.send(any(ProducerRecord.class))).willReturn(CompletableFuture.completedFuture(recordMetadata));

        DLQEntry entry1 = createTestEntry("error-1", "topic", "Error 1", "VALIDATION_ERROR");
        DLQEntry entry2 = createTestEntry("error-2", "topic", "Error 2", "PROCESSING_ERROR");
        DLQEntry entry3 = createTestEntry("error-3", "topic", "Error 3", "VALIDATION_ERROR");

        // When
        dlq.send(entry1);
        dlq.send(entry2);
        dlq.send(entry3);

        // Then
        DLQStats stats = dlq.getStats();
        Map<String, Long> byErrorType = stats.getMessagesByErrorType();
        assertThat(byErrorType.get("VALIDATION_ERROR")).isEqualTo(2);
        assertThat(byErrorType.get("PROCESSING_ERROR")).isEqualTo(1);
    }

    @Test
    @DisplayName("DLQEntry should create correctly")
    void dlqEntryShouldCreateCorrectly() {
        // When
        DLQEntry entry = DLQEntry.create("test.topic", "Test error", "TEST_ERROR");

        // Then
        assertThat(entry.getTopic()).isEqualTo("test.topic");
        assertThat(entry.getErrorMessage()).isEqualTo("Test error");
        assertThat(entry.getErrorType()).isEqualTo("TEST_ERROR");
        assertThat(entry.getRetryCount()).isEqualTo(0);
        assertThat(entry.getTimestamp()).isNotNull();
        assertThat(entry.getAddedAt()).isNotNull();
    }

    @Test
    @DisplayName("DLQEntry should create from failed message")
    void dlqEntryShouldCreateFromFailedMessage() {
        // When
        DLQEntry entry = DLQEntry.fromFailedMessage("test.topic", "Failed", "FAILED", "payload");

        // Then
        assertThat(entry.getTopic()).isEqualTo("test.topic");
        assertThat(entry.getOriginalPayload()).isEqualTo("payload");
    }

    @Test
    @DisplayName("DLQEntry should create with exception")
    void dlqEntryShouldCreateWithException() {
        // Given
        RuntimeException exception = new RuntimeException("Test exception");

        // When
        DLQEntry entry = DLQEntry.withException("test.topic", "Failed", "FAILED", "payload", exception);

        // Then
        assertThat(entry.getExceptionType()).isEqualTo("java.lang.RuntimeException");
        assertThat(entry.getStackTrace()).isNotNull();
    }

    @Test
    @DisplayName("DLQEntry should calculate age and time in DLQ")
    void dlqEntryShouldCalculateAgeAndTimeInDLQ() throws InterruptedException {
        // Given
        DLQEntry entry = DLQEntry.create("test.topic", "Error", "ERROR");
        long startTime = System.currentTimeMillis();

        Thread.sleep(100);

        // When
        long ageMs = entry.getAgeMs();
        long timeInDLQMs = entry.getTimeInDLQMs();

        // Then
        assertThat(ageMs).isGreaterThan(0);
        assertThat(timeInDLQMs).isGreaterThan(0);
        assertThat(timeInDLQMs).isGreaterThanOrEqualTo(ageMs);
    }

    @Test
    @DisplayName("DLQEntry should track headers")
    void dlqEntryShouldTrackHeaders() {
        // Given
        DLQEntry entry = DLQEntry.newBuilder()
            .topic("test.topic")
            .errorMessage("Error")
            .errorType("ERROR")
            .addHeader("header1", "value1")
            .addHeader("header2", 123)
            .build();

        // Then
        assertThat(entry.hasHeader("header1")).isTrue();
        assertThat(entry.hasHeader("header3")).isFalse();
        assertThat(entry.getHeader("header1")).isEqualTo("value1");
        assertThat(entry.getHeader("header2")).isEqualTo(123);
        assertThat(entry.getHeader("header3", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("FixedDelayRetryPolicy should work correctly")
    void fixedDelayRetryPolicyShouldWorkCorrectly() {
        // Given
        RetryPolicy policy = new FixedDelayRetryPolicy(3, 1000, false);
        DLQEntry entry = createTestEntry("retry", "topic", "Error", "ERROR");

        // When
        entry = DLQEntry.newBuilder()
            .topic(entry.getTopic())
            .errorMessage(entry.getErrorMessage())
            .errorType(entry.getErrorType())
            .retryCount(2)
            .build();

        // Then
        assertThat(policy.shouldRetry(entry)).isTrue();
        assertThat(policy.getRetryDelay(entry)).isEqualTo(1000);
        assertThat(policy.getMaxRetries()).isEqualTo(3);

        // When retry count exceeds max
        DLQEntry entryExhausted = DLQEntry.newBuilder()
            .topic(entry.getTopic())
            .errorMessage(entry.getErrorMessage())
            .errorType(entry.getErrorType())
            .retryCount(5)
            .build();

        assertThat(policy.shouldRetry(entryExhausted)).isFalse();
    }

    @Test
    @DisplayName("FixedDelayRetryPolicy should use exponential backoff")
    void fixedDelayRetryPolicyShouldUseExponentialBackoff() {
        // Given
        RetryPolicy policy = new FixedDelayRetryPolicy(5, 1000, true);
        DLQEntry entry1 = createTestEntry("retry-1", "topic", "Error", "ERROR");
        DLQEntry entry2 = createTestEntry("retry-2", "topic", "Error", "ERROR");

        entry1 = DLQEntry.newBuilder()
            .topic(entry1.getTopic())
            .errorMessage(entry1.getErrorMessage())
            .errorType(entry1.getErrorType())
            .retryCount(2)
            .build();

        entry2 = DLQEntry.newBuilder()
            .topic(entry2.getTopic())
            .errorMessage(entry2.getErrorMessage())
            .errorType(entry2.getErrorType())
            .retryCount(3)
            .build();

        // Then
        assertThat(policy.getRetryDelay(entry1)).isEqualTo(4000); // 1000 * 2^2
        assertThat(policy.getRetryDelay(entry2)).isEqualTo(8000); // 1000 * 2^3
    }

    @Test
    @DisplayName("DeadLetterQueueException should create specific exceptions")
    void deadLetterQueueExceptionShouldCreateSpecificExceptions() {
        // Test message not found
        DeadLetterQueueException notFound = DeadLetterQueueException.messageNotFound("msg-123", "test-dlq");
        assertThat(notFound.getMessage()).contains("Message not found");
        assertThat(notFound.getErrorCode()).isEqualTo("MESSAGE_NOT_FOUND");
        assertThat(notFound.isRetryable()).isFalse();

        // Test queue full
        DeadLetterQueueException queueFull = DeadLetterQueueException.queueFull("test-dlq", 1000, 500);
        assertThat(queueFull.getMessage()).contains("Queue is full");
        assertThat(queueFull.getErrorCode()).isEqualTo("QUEUE_FULL");
    }

    // Test helper methods
    private DLQEntry createTestEntry(String messageId, String topic, String errorMessage, String errorType) {
        return DLQEntry.newBuilder()
            .messageId(messageId)
            .topic(topic)
            .errorMessage(errorMessage)
            .errorType(errorType)
            .timestamp(Instant.now())
            .build();
    }
}
