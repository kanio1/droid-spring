package com.droid.bss.infrastructure.messaging.deadletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Processes messages from the Dead Letter Queue.
 *
 * @since 1.0
 */
public class DLQMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(DLQMessageProcessor.class);

    private final DeadLetterQueue deadLetterQueue;
    private final RetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new DLQMessageProcessor.
     *
     * @param deadLetterQueue the DLQ
     * @param retryPolicy the retry policy
     */
    public DLQMessageProcessor(DeadLetterQueue deadLetterQueue, RetryPolicy retryPolicy) {
        this.deadLetterQueue = deadLetterQueue;
        this.retryPolicy = retryPolicy;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Processes a DLQ message.
     *
     * @param payload the message payload
     * @param topic the topic
     * @param partition the partition
     * @param offset the offset
     * @param acknowledgment the acknowledgment
     */
    @KafkaListener(topics = "#{@dlqTopic.name()}", containerFactory = "dlqListenerContainerFactory")
    public void processDLQMessage(@Payload String payload,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset,
                                  Acknowledgment acknowledgment) {
        try {
            log.debug("Processing DLQ message: topic={}, partition={}, offset={}", topic, partition, offset);

            // Deserialize the DLQ entry
            DLQEntry entry = deserializeEntry(payload);
            if (entry == null) {
                log.error("Failed to deserialize DLQ entry: topic={}, partition={}, offset={}", topic, partition, offset);
                acknowledgment.acknowledge();
                return;
            }

            // Check if we should retry
            if (retryPolicy.shouldRetry(entry)) {
                long delay = retryPolicy.getRetryDelay(entry);

                log.info("Retrying DLQ message: messageId={}, delay={}ms", entry.getMessageId(), delay);

                // In a real implementation, you would requeue the message
                // For now, we just log it

            } else {
                log.warn("Message will not be retried: messageId={}, errorType={}, retryCount={}",
                    entry.getMessageId(), entry.getErrorType(), entry.getRetryCount());
            }

            // Acknowledge the message
            acknowledgment.acknowledge();

            log.debug("Processed DLQ message: messageId={}", entry.getMessageId());

        } catch (Exception e) {
            log.error("Error processing DLQ message: topic={}, partition={}, offset={}, error={}",
                topic, partition, offset, e.getMessage(), e);
            // Acknowledge to avoid infinite loop
            acknowledgment.acknowledge();
        }
    }

    /**
     * Processes multiple DLQ messages in batch.
     *
     * @param messages the list of messages
     * @param acknowledgment the acknowledgment
     */
    public void processBatch(List<String> messages, Acknowledgment acknowledgment) {
        try {
            log.debug("Processing DLQ batch: size={}", messages.size());

            for (String payload : messages) {
                try {
                    DLQEntry entry = deserializeEntry(payload);
                    if (entry != null) {
                        log.debug("Processing batch entry: messageId={}", entry.getMessageId());
                    }
                } catch (Exception e) {
                    log.error("Error processing batch entry: {}", e.getMessage(), e);
                }
            }

            acknowledgment.acknowledge();
            log.debug("Processed DLQ batch: size={}", messages.size());

        } catch (Exception e) {
            log.error("Error processing DLQ batch: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private DLQEntry deserializeEntry(String payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);

            DLQEntry.Builder builder = DLQEntry.newBuilder()
                .messageId(getString(data, "messageId"))
                .topic(getString(data, "topic"))
                .partition(getInt(data, "partition", -1))
                .offset(getLong(data, "offset", -1L))
                .errorMessage(getString(data, "errorMessage"))
                .errorCode(getString(data, "errorCode"))
                .errorType(getString(data, "errorType"))
                .retryCount(getInt(data, "retryCount", 0))
                .timestamp(parseInstant(getString(data, "timestamp")))
                .originalPayload(getString(data, "originalPayload"))
                .originalMessageKey(getString(data, "originalMessageKey"))
                .exceptionType(getString(data, "exceptionType"))
                .stackTrace(getString(data, "stackTrace"));

            @SuppressWarnings("unchecked")
            Map<String, Object> headers = (Map<String, Object>) data.get("headers");
            if (headers != null) {
                builder.headers(headers);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to deserialize DLQ entry: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getString(Map<String, Object> data, String key) {
        return data.containsKey(key) ? (String) data.get(key) : null;
    }

    private int getInt(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> data, String key, long defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return defaultValue;
    }

    private Instant parseInstant(String timestamp) {
        if (timestamp == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(timestamp);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
