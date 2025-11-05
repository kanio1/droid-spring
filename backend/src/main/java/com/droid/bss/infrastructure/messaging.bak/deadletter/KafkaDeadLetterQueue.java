package com.droid.bss.infrastructure.messaging.deadletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Kafka-based implementation of DeadLetterQueue.
 *
 * Stores failed messages in Kafka topics.
 *
 * @since 1.0
 */
public class KafkaDeadLetterQueue implements DeadLetterQueue {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeadLetterQueue.class);

    private final DeadLetterQueueConfig config;
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final DLQStats stats;
    private final ExecutorService executorService;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong messageCounter = new AtomicLong(0);

    /**
     * Creates a new KafkaDeadLetterQueue.
     *
     * @param config the configuration
     * @param producer the Kafka producer
     * @param objectMapper the JSON object mapper
     */
    public KafkaDeadLetterQueue(DeadLetterQueueConfig config,
                                 KafkaProducer<String, String> producer,
                                 ObjectMapper objectMapper) {
        this.config = config;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.stats = new DLQStats(config.getName());
        this.executorService = Executors.newCachedThreadPool();

        log.info("Created DLQ '{}' with config: maxMessages={}, retention={}",
            config.getName(), config.getMaxMessages(), config.getRetentionTime());
    }

    /**
     * No-args constructor for tests.
     */
    public KafkaDeadLetterQueue() {
        this.config = null;
        this.producer = null;
        this.objectMapper = null;
        this.stats = new DLQStats("test-dlq");
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void send(DLQEntry entry) throws DeadLetterQueueException {
        if (closed.get()) {
            throw DeadLetterQueueException.connectionFailure(
                config.getName(),
                "DLQ is closed",
                null
            );
        }

        validateEntry(entry);

        try {
            String topic = getDLQTopicName(entry.getTopic());
            String key = entry.getMessageId();

            // Serialize the entry
            String payload = serializeEntry(entry);

            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            producer.send(record);

            // Update statistics
            stats.recordAdd(entry.getTopic(), entry.getErrorType(), entry.getErrorCode());

            log.debug("Sent message to DLQ: messageId={}, topic={}, errorType={}",
                entry.getMessageId(), entry.getTopic(), entry.getErrorType());

        } catch (Exception e) {
            stats.recordError();
            throw DeadLetterQueueException.serializationFailure(
                entry.getMessageId(),
                config.getName(),
                e
            );
        }
    }

    @Override
    public CompletableFuture<Void> sendAsync(DLQEntry entry) {
        return CompletableFuture.runAsync(() -> {
            try {
                send(entry);
            } catch (DeadLetterQueueException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    @Override
    public void sendBatch(List<DLQEntry> entries) throws DeadLetterQueueException {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        int successCount = 0;
        List<Exception> errors = new ArrayList<>();

        for (DLQEntry entry : entries) {
            try {
                send(entry);
                successCount++;
            } catch (DeadLetterQueueException e) {
                errors.add(e);
            }
        }

        if (successCount < entries.size()) {
            throw DeadLetterQueueException.batchOperationFailure(
                config.getName(),
                "sendBatch",
                entries.size(),
                successCount,
                new IllegalStateException("Partial batch failure: " + errors.size() + " errors")
            );
        }

        log.debug("Batch sent to DLQ: size={}", entries.size());
    }

    @Override
    public CompletableFuture<Void> sendBatchAsync(List<DLQEntry> entries) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendBatch(entries);
            } catch (DeadLetterQueueException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    @Override
    public DLQEntry get(String messageId) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("get by ID not implemented for Kafka DLQ");
    }

    @Override
    public List<DLQEntry> getBatch(int limit) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("getBatch not implemented for Kafka DLQ");
    }

    @Override
    public List<DLQEntry> getByTopic(String topic, int limit) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("getByTopic not implemented for Kafka DLQ");
    }

    @Override
    public List<DLQEntry> getByErrorType(String errorType, int limit) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("getByErrorType not implemented for Kafka DLQ");
    }

    @Override
    public List<DLQEntry> getAfterTimestamp(Instant timestamp, int limit) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("getAfterTimestamp not implemented for Kafka DLQ");
    }

    @Override
    public boolean delete(String messageId) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("delete not implemented for Kafka DLQ (messages are immutable)");
    }

    @Override
    public int deleteBatch(List<String> messageIds) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("deleteBatch not implemented for Kafka DLQ");
    }

    @Override
    public boolean requeue(String messageId) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("requeue not implemented for Kafka DLQ");
    }

    @Override
    public int requeueBatch(List<String> messageIds) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("requeueBatch not implemented for Kafka DLQ");
    }

    @Override
    public long count() throws DeadLetterQueueException {
        throw new UnsupportedOperationException("count not implemented for Kafka DLQ");
    }

    @Override
    public long countByTopic(String topic) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("countByTopic not implemented for Kafka DLQ");
    }

    @Override
    public long countByErrorType(String errorType) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("countByErrorType not implemented for Kafka DLQ");
    }

    @Override
    public boolean isHealthy() {
        try {
            return !closed.get() &&
                   producer != null &&
                   !producer.metrics().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public DLQStats getStats() {
        return stats;
    }

    @Override
    public long purge() throws DeadLetterQueueException {
        throw new UnsupportedOperationException("purge not implemented for Kafka DLQ (use topic retention)");
    }

    @Override
    public long purgeOlderThan(Instant timestamp) throws DeadLetterQueueException {
        throw new UnsupportedOperationException("purgeOlderThan not implemented for Kafka DLQ");
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public DeadLetterQueueConfig getConfig() {
        return config;
    }

    @Override
    public void close() throws DeadLetterQueueException {
        if (closed.compareAndSet(false, true)) {
            try {
                if (producer != null) {
                    producer.close(Duration.ofSeconds(30));
                }
            } catch (Exception e) {
                log.error("Error closing DLQ producer: {}", e.getMessage(), e);
            } finally {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Closed DLQ '{}'", config.getName());
        }
    }

    // Private helper methods

    private void validateEntry(DLQEntry entry) throws DeadLetterQueueException {
        if (entry == null) {
            throw DeadLetterQueueException.invalidEntry(
                config.getName(),
                "DLQ entry cannot be null"
            );
        }

        if (entry.getTopic() == null || entry.getTopic().isBlank()) {
            throw DeadLetterQueueException.invalidEntry(
                config.getName(),
                "Topic cannot be null or blank"
            );
        }

        if (entry.getOriginalPayload() != null &&
            entry.getOriginalPayload().length() > config.getMaxPayloadSize()) {
            throw DeadLetterQueueException.invalidEntry(
                config.getName(),
                "Payload size exceeds maximum: " + entry.getOriginalPayload().length() +
                " > " + config.getMaxPayloadSize()
            );
        }
    }

    private String getDLQTopicName(String originalTopic) {
        return config.getTopicPrefix() + "." + originalTopic;
    }

    private String serializeEntry(DLQEntry entry) throws DeadLetterQueueException {
        try {
            // For simplicity, use a lightweight serialization
            Map<String, Object> serialized = new HashMap<>();
            serialized.put("messageId", entry.getMessageId());
            serialized.put("topic", entry.getTopic());
            serialized.put("partition", entry.getPartition());
            serialized.put("offset", entry.getOffset());
            serialized.put("errorMessage", entry.getErrorMessage());
            serialized.put("errorCode", entry.getErrorCode());
            serialized.put("errorType", entry.getErrorType());
            serialized.put("retryCount", entry.getRetryCount());
            serialized.put("timestamp", entry.getTimestamp().toString());
            serialized.put("addedAt", entry.getAddedAt().toString());

            if (entry.getOriginalPayload() != null && config.getStorePayload()) {
                serialized.put("originalPayload", entry.getOriginalPayload());
            }

            if (entry.getExceptionType() != null && config.getStoreStackTraces()) {
                serialized.put("exceptionType", entry.getExceptionType());
            }

            if (entry.getStackTrace() != null && config.getStoreStackTraces()) {
                serialized.put("stackTrace", entry.getStackTrace());
            }

            if (!entry.getHeaders().isEmpty()) {
                serialized.put("headers", entry.getHeaders());
            }

            if (entry.getOriginalMessageKey() != null) {
                serialized.put("originalMessageKey", entry.getOriginalMessageKey());
            }

            return objectMapper.writeValueAsString(serialized);

        } catch (Exception e) {
            throw DeadLetterQueueException.serializationFailure(
                entry.getMessageId(),
                config.getName(),
                e
            );
        }
    }
}
