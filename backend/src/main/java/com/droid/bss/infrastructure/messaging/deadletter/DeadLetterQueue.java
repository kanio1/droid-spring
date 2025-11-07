package com.droid.bss.infrastructure.messaging.deadletter;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dead Letter Queue (DLQ) Handler
 *
 * Handles messages that have failed processing in event consumers.
 * Stores failed events for later analysis, reprocessing, or manual intervention.
 */
@Component
public class DeadLetterQueue {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterQueue.class);

    private final ConcurrentMap<String, DLQEntry> failedEvents = new ConcurrentHashMap<>();
    private final AtomicLong totalFailedEvents = new AtomicLong(0);
    private final AtomicLong totalReprocessedEvents = new AtomicLong(0);

    /**
     * Store a failed event in the DLQ
     */
    public void storeFailedEvent(String eventId, String eventType, CloudEvent event,
                                 String errorMessage, String originalTopic,
                                 int partition, long offset, int retryCount) {
        DLQEntry entry = new DLQEntry(
                eventId,
                eventType,
                event,
                errorMessage,
                originalTopic,
                partition,
                offset,
                retryCount,
                LocalDateTime.now(),
                DLQStatus.PENDING
        );

        failedEvents.put(eventId, entry);
        totalFailedEvents.incrementAndGet();

        log.error("Event stored in DLQ: {} - {} from topic: {} partition: {} offset: {}. Error: {}",
                eventType, eventId, originalTopic, partition, offset, errorMessage);
    }

    /**
     * Get all failed events
     */
    public ConcurrentMap<String, DLQEntry> getFailedEvents() {
        return new ConcurrentHashMap<>(failedEvents);
    }

    /**
     * Get failed events by status
     */
    public ConcurrentMap<String, DLQEntry> getFailedEventsByStatus(DLQStatus status) {
        ConcurrentMap<String, DLQEntry> filtered = new ConcurrentHashMap<>();
        failedEvents.entrySet().parallelStream()
                .filter(entry -> entry.getValue().getStatus() == status)
                .forEach(entry -> filtered.put(entry.getKey(), entry.getValue()));
        return filtered;
    }

    /**
     * Reprocess a failed event
     */
    public boolean reprocessEvent(String eventId) {
        DLQEntry entry = failedEvents.get(eventId);
        if (entry == null) {
            log.warn("Cannot reprocess event - not found in DLQ: {}", eventId);
            return false;
        }

        if (entry.getStatus() == DLQStatus.REPROCESSED) {
            log.warn("Event already reprocessed: {}", eventId);
            return false;
        }

        entry.setStatus(DLQStatus.REPROCESSED);
        entry.setReprocessedAt(LocalDateTime.now());
        totalReprocessedEvents.incrementAndGet();

        log.info("Event marked for reprocessing: {}", eventId);
        return true;
    }

    /**
     * Mark event as resolved (after manual review/fix)
     */
    public boolean markAsResolved(String eventId, String resolution) {
        DLQEntry entry = failedEvents.get(eventId);
        if (entry == null) {
            log.warn("Cannot resolve event - not found in DLQ: {}", eventId);
            return false;
        }

        entry.setStatus(DLQStatus.RESOLVED);
        entry.setResolution(resolution);
        entry.setResolvedAt(LocalDateTime.now());

        log.info("Event marked as resolved: {}. Resolution: {}", eventId, resolution);
        return true;
    }

    /**
     * Clean up old DLQ entries (older than 7 days)
     */
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        failedEvents.entrySet().removeIf(entry -> {
            if (entry.getValue().getStatus() == DLQStatus.RESOLVED &&
                entry.getValue().getResolvedAt().isBefore(cutoff)) {
                log.info("Cleaning up old DLQ entry: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Get statistics
     */
    public DLQStatistics getStatistics() {
        return new DLQStatistics(
                totalFailedEvents.get(),
                totalReprocessedEvents.get(),
                failedEvents.size(),
                failedEvents.values().parallelStream()
                        .filter(e -> e.getStatus() == DLQStatus.PENDING)
                        .count(),
                failedEvents.values().parallelStream()
                        .filter(e -> e.getStatus() == DLQStatus.RESOLVED)
                        .count()
        );
    }

    // Kafka listener for monitoring DLQ (can be used for alerts)
    @KafkaListener(
            topics = {"dlq.monitoring"},
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDLQMonitoringEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, CloudEvent> record,
            Acknowledgment acknowledgment
    ) {
        int partition = record.partition();
        long offset = record.offset();

        log.info("DLQ monitoring event received: {} from topic: {} partition: {} offset: {}",
                event.getType(), topic, partition, offset);
        acknowledgment.acknowledge();
    }
}
