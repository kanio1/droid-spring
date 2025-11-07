package com.droid.bss.infrastructure.messaging;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.infrastructure.messaging.deadletter.DeadLetterQueue;
import com.droid.bss.infrastructure.cache.CacheEvictionService;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Customer Event Consumer
 *
 * Handles CloudEvents for customer domain:
 * - com.droid.bss.customer.created.v1
 * - com.droid.bss.customer.updated.v1
 * - com.droid.bss.customer.statusChanged.v1
 * - com.droid.bss.customer.terminated.v1
 */
@Component
public class CustomerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventConsumer.class);

    // Event deduplication cache (key: event ID, value: processed timestamp)
    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsRetried = new AtomicLong(0);
    private final AtomicLong totalEventsSentToDLQ = new AtomicLong(0);

    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final DeadLetterQueue deadLetterQueue;
    private final CacheEvictionService cacheEvictionService;

    /**
     * Constructor with dependency injection
     */
    public CustomerEventConsumer(DeadLetterQueue deadLetterQueue, CacheEvictionService cacheEvictionService) {
        this.deadLetterQueue = deadLetterQueue;
        this.cacheEvictionService = cacheEvictionService;
    }

    /**
     * Main Kafka listener for customer events
     */
    @KafkaListener(
            topics = {
                    "customer.created",
                    "customer.updated",
                    "customer.statusChanged",
                    "customer.terminated"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCustomerEvent(
            @Payload CloudEvent cloudEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, CloudEvent> record,
            Acknowledgment acknowledgment
    ) {
        int partition = record.partition();
        long offset = record.offset();
        String eventId = cloudEvent.getId();
        String eventType = cloudEvent.getType();

        try {
            log.info("Received customer event: {} from topic: {} partition: {} offset: {}",
                    eventType, topic, partition, offset);

            // Check for duplicate event (idempotency)
            if (isDuplicateEvent(eventId)) {
                log.warn("Duplicate event detected, skipping: {}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Process event based on type
            CompletableFuture<Void> processingFuture = processEvent(cloudEvent, topic);

            processingFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleProcessingFailure(eventId, eventType, throwable, acknowledgment, topic, partition, offset, cloudEvent);
                } else {
                    handleProcessingSuccess(eventId, eventType, acknowledgment);
                }
            });

        } catch (Exception e) {
            log.error("Unexpected error processing customer event: {} from topic: {} offset: {}",
                    eventType, topic, offset, e);
            totalEventsFailed.incrementAndGet();
            // Don't acknowledge on unexpected errors, let it retry
        }
    }

    /**
     * Check if event has already been processed (idempotency)
     */
    private boolean isDuplicateEvent(String eventId) {
        // Check if event ID is in our processed cache
        if (processedEventIds.containsKey(eventId)) {
            return true;
        }

        // Add to cache with current timestamp
        processedEventIds.put(eventId, System.currentTimeMillis());

        // Clean up old entries (older than 1 hour)
        long cutoff = System.currentTimeMillis() - 3_600_000; // 1 hour
        processedEventIds.entrySet().removeIf(entry -> entry.getValue() < cutoff);

        return false;
    }

    /**
     * Process event based on type
     */
    private CompletableFuture<Void> processEvent(CloudEvent cloudEvent, String topic) {
        return CompletableFuture.runAsync(() -> {
            String eventType = cloudEvent.getType();

            try {
                switch (eventType) {
                    case "com.droid.bss.customer.created.v1":
                        handleCustomerCreated(cloudEvent);
                        break;
                    case "com.droid.bss.customer.updated.v1":
                        handleCustomerUpdated(cloudEvent);
                        break;
                    case "com.droid.bss.customer.statusChanged.v1":
                        handleCustomerStatusChanged(cloudEvent);
                        break;
                    case "com.droid.bss.customer.terminated.v1":
                        handleCustomerTerminated(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown customer event type: {}", eventType);
                }

                totalEventsProcessed.incrementAndGet();

            } catch (Exception e) {
                log.error("Error processing customer event: {} - {}",
                        eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    /**
     * Handle customer created event
     */
    private void handleCustomerCreated(CloudEvent cloudEvent) {
        log.info("Handling customer created event: {}", cloudEvent.getId());

        // TODO: Implement actual business logic
        // - Update read model
        // - Send notifications
        // - Update search indices
        // - Trigger downstream processes

        // Evict all customer caches to ensure fresh data
        cacheEvictionService.evictAllCustomerListCaches();

        log.debug("Customer created event processed successfully");
    }

    /**
     * Handle customer updated event
     */
    private void handleCustomerUpdated(CloudEvent cloudEvent) {
        log.info("Handling customer updated event: {}", cloudEvent.getId());

        // TODO: Implement actual business logic
        // - Update read model
        // - Update search indices
        // - Send notifications if needed

        // Evict specific customer cache by ID from event data
        // In a real implementation, you would parse the event data to get the customer ID
        // cacheEvictionService.evictCustomerCache(customerIdFromEvent);

        // Evict all customer caches to ensure consistency
        cacheEvictionService.evictAllCustomerListCaches();

        log.debug("Customer updated event processed successfully");
    }

    /**
     * Handle customer status changed event
     */
    private void handleCustomerStatusChanged(CloudEvent cloudEvent) {
        log.info("Handling customer status changed event: {}", cloudEvent.getId());

        // TODO: Implement actual business logic
        // - Update read model with new status
        // - Handle status-specific actions (e.g., suspend services)
        // - Send notifications
        // - Update related entities

        // Evict all customer caches
        cacheEvictionService.evictAllCustomerListCaches();

        log.debug("Customer status changed event processed successfully");
    }

    /**
     * Handle customer terminated event
     */
    private void handleCustomerTerminated(CloudEvent cloudEvent) {
        log.info("Handling customer terminated event: {}", cloudEvent.getId());

        // TODO: Implement actual business logic
        // - Update read model
        // - Cancel active subscriptions
        // - Generate final invoices
        // - Send termination confirmation
        // - Archive customer data

        // Evict all customer caches
        cacheEvictionService.evictAllCustomerListCaches();

        log.debug("Customer terminated event processed successfully");
    }

    /**
     * Handle successful processing
     */
    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed customer event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    /**
     * Handle processing failure with retry logic and DLQ
     */
    private void handleProcessingFailure(
            String eventId,
            String eventType,
            Throwable throwable,
            Acknowledgment acknowledgment,
            String topic,
            int partition,
            long offset,
            CloudEvent cloudEvent
    ) {
        log.error("Failed to process customer event: {} - {} from topic: {} partition: {} offset: {}. Error: {}",
                eventType, eventId, topic, partition, offset, throwable.getMessage(), throwable);

        totalEventsFailed.incrementAndGet();

        // Send to Dead Letter Queue
        // Note: In a real implementation, you would track retry count per event
        // For simplicity, we're sending all failures to DLQ
        sendToDeadLetterQueue(eventId, eventType, throwable, topic, partition, offset, cloudEvent);

        // Acknowledge to prevent infinite retries
        acknowledgment.acknowledge();
    }

    /**
     * Send failed event to Dead Letter Queue
     */
    private void sendToDeadLetterQueue(
            String eventId,
            String eventType,
            Throwable throwable,
            String topic,
            int partition,
            long offset,
            CloudEvent cloudEvent
    ) {
        try {
            log.error("Sending failed event to DLQ: {} - {} from topic: {} partition: {} offset: {}. Error: {}",
                    eventType, eventId, topic, partition, offset, throwable.getMessage(), throwable);

            // Store the failed event in DLQ with full details
            deadLetterQueue.storeFailedEvent(
                    eventId,
                    eventType,
                    cloudEvent,
                    throwable.getMessage(),
                    topic,
                    partition,
                    offset,
                    0
            );

            totalEventsSentToDLQ.incrementAndGet();

            log.error("Event {} sent to Dead Letter Queue for manual review and reprocessing", eventId);

        } catch (Exception e) {
            log.error("Failed to send event to DLQ: {} - {}", eventId, e.getMessage(), e);
        }
    }

    // Metrics getters for monitoring

    public long getTotalEventsProcessed() {
        return totalEventsProcessed.get();
    }

    public long getTotalEventsFailed() {
        return totalEventsFailed.get();
    }

    public long getTotalEventsRetried() {
        return totalEventsRetried.get();
    }

    public double getSuccessRate() {
        long total = totalEventsProcessed.get() + totalEventsFailed.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalEventsProcessed.get() / total * 100.0;
    }

    public int getDuplicateEventCount() {
        return processedEventIds.size();
    }

    public long getTotalEventsSentToDLQ() {
        return totalEventsSentToDLQ.get();
    }
}
