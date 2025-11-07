package com.droid.bss.infrastructure.outbox;

import com.droid.bss.domain.outbox.OutboxEvent;
import com.droid.bss.domain.outbox.OutboxEventType;
import com.droid.bss.domain.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Outbox Event Publisher
 *
 * Implements reliable event publishing using the Outbox Pattern
 * Periodically publishes pending events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String defaultTopic = "bss.events";

    /**
     * Publish a single event to outbox
     */
    @Transactional
    public OutboxEvent publishEvent(
            OutboxEventType eventType,
            String eventName,
            String aggregateId,
            String aggregateType,
            Object eventData,
            String correlationId,
            String causationId,
            String userId) {

        return publishEvent(
                eventType,
                eventName,
                aggregateId,
                aggregateType,
                eventData,
                correlationId,
                causationId,
                userId,
                defaultTopic
        );
    }

    /**
     * Publish a single event to outbox with custom topic
     */
    @Transactional
    public OutboxEvent publishEvent(
            OutboxEventType eventType,
            String eventName,
            String aggregateId,
            String aggregateType,
            Object eventData,
            String correlationId,
            String causationId,
            String userId,
            String topic) {

        try {
            // Convert event data to JSON
            String eventDataJson = serializeEventData(eventData);
            String metadataJson = serializeEventData(createMetadata());

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .eventName(eventName)
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventData(eventDataJson)
                    .metadata(metadataJson)
                    .version("1.0")
                    .source("BSS-System")
                    .correlationId(correlationId)
                    .causationId(causationId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .maxRetries(3)
                    .traceId(correlationId)
                    .build();

            OutboxEvent saved = outboxRepository.save(outboxEvent);
            log.debug("Event saved to outbox: {} (ID: {})", eventName, saved.getId());
            return saved;

        } catch (Exception e) {
            log.error("Failed to save event to outbox: {}", eventName, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Scheduled task to publish pending events
     * Runs every 5 seconds
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository
                .findPendingEventsForProcessing(OutboxStatus.PENDING, LocalDateTime.now(), null);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending events to publish", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                publishEventToKafka(event);
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getEventName(), e);
                // Let the exception mark the event as failed
            }
        }
    }

    /**
     * Publish a specific event to Kafka
     */
    @Transactional
    public void publishEventToKafka(OutboxEvent event) {
        try {
            String topic = defaultTopic;
            String key = event.getAggregateId() != null ? event.getAggregateId() : event.getEventId();

            // Send to Kafka
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);

            // Handle result asynchronously
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    markEventAsPublished(event);
                    log.info("Event published successfully: {} (ID: {})", event.getEventName(), event.getId());
                } else {
                    markEventAsFailed(event, exception.getMessage());
                    log.error("Failed to publish event: {} (ID: {})", event.getEventName(), event.getId(), exception);
                }
            });

        } catch (Exception e) {
            markEventAsFailed(event, e.getMessage());
            throw e;
        }
    }

    /**
     * Manually retry a failed event
     */
    @Transactional
    public void retryEvent(UUID eventId) {
        OutboxEvent event = outboxRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        if (event.isDeadLetter()) {
            throw new IllegalStateException("Cannot retry dead letter event: " + eventId);
        }

        if (event.isPending()) {
            throw new IllegalStateException("Event is already pending: " + eventId);
        }

        // Reset for retry
        event.setStatus(OutboxStatus.PENDING);
        event.setErrorMessage(null);
        event.setNextRetryAt(null);

        outboxRepository.save(event);
        log.info("Event reset for retry: {}", eventId);

        // Publish immediately
        publishEventToKafka(event);
    }

    /**
     * Mark event as published
     */
    @Transactional
    public void markEventAsPublished(OutboxEvent event) {
        event.markAsPublished();
        outboxRepository.save(event);
    }

    /**
     * Mark event as failed
     */
    @Transactional
    public void markEventAsFailed(OutboxEvent event, String errorMessage) {
        event.markAsFailed(errorMessage);
        outboxRepository.save(event);
    }

    /**
     * Get outbox statistics
     */
    public OutboxStatistics getStatistics() {
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
        long failed = outboxRepository.countByStatus(OutboxStatus.DEAD_LETTER);

        return new OutboxStatistics(pending, published, failed, pending + published + failed);
    }

    /**
     * Serialize event data to JSON
     */
    private String serializeEventData(Object data) {
        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }

    /**
     * Create metadata for event
     */
    private Object createMetadata() {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "source", "BSS-System",
                "version", "1.0"
        );
    }

    /**
     * Outbox Statistics
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OutboxStatistics {
        private long pending;
        private long published;
        private long failed;
        private long total;
    }
}
