package com.droid.bss.infrastructure.messaging;

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
 * Order Event Consumer
 *
 * Handles CloudEvents for order domain:
 * - com.droid.bss.order.created.v1
 * - com.droid.bss.order.updated.v1
 * - com.droid.bss.order.statusChanged.v1
 * - com.droid.bss.order.completed.v1
 * - com.droid.bss.order.cancelled.v1
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    // Event deduplication cache
    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final AtomicLong totalEventsRetried = new AtomicLong(0);
    private final CacheEvictionService cacheEvictionService;

    public OrderEventConsumer(CacheEvictionService cacheEvictionService) {
        this.cacheEvictionService = cacheEvictionService;
    }

    @KafkaListener(
            topics = {
                    "order.created",
                    "order.updated",
                    "order.statusChanged",
                    "order.completed",
                    "order.cancelled"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
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
            log.info("Received order event: {} from topic: {} partition: {} offset: {}",
                    eventType, topic, partition, offset);

            if (isDuplicateEvent(eventId)) {
                log.warn("Duplicate event detected, skipping: {}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            CompletableFuture<Void> processingFuture = processEvent(cloudEvent, topic);

            processingFuture.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    handleProcessingFailure(eventId, eventType, throwable, acknowledgment);
                } else {
                    handleProcessingSuccess(eventId, eventType, acknowledgment);
                }
            });

        } catch (Exception e) {
            log.error("Unexpected error processing order event: {} from topic: {} offset: {}",
                    eventType, topic, offset, e);
            totalEventsFailed.incrementAndGet();
        }
    }

    private boolean isDuplicateEvent(String eventId) {
        if (processedEventIds.containsKey(eventId)) {
            return true;
        }

        processedEventIds.put(eventId, System.currentTimeMillis());

        long cutoff = System.currentTimeMillis() - 3_600_000; // 1 hour
        processedEventIds.entrySet().removeIf(entry -> entry.getValue() < cutoff);

        return false;
    }

    private CompletableFuture<Void> processEvent(CloudEvent cloudEvent, String topic) {
        return CompletableFuture.runAsync(() -> {
            String eventType = cloudEvent.getType();

            try {
                switch (eventType) {
                    case "com.droid.bss.order.created.v1":
                        handleOrderCreated(cloudEvent);
                        break;
                    case "com.droid.bss.order.updated.v1":
                        handleOrderUpdated(cloudEvent);
                        break;
                    case "com.droid.bss.order.statusChanged.v1":
                        handleOrderStatusChanged(cloudEvent);
                        break;
                    case "com.droid.bss.order.completed.v1":
                        handleOrderCompleted(cloudEvent);
                        break;
                    case "com.droid.bss.order.cancelled.v1":
                        handleOrderCancelled(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown order event type: {}", eventType);
                }

                totalEventsProcessed.incrementAndGet();

            } catch (Exception e) {
                log.error("Error processing order event: {} - {}",
                        eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    private void handleOrderCreated(CloudEvent cloudEvent) {
        log.info("Handling order created event: {}", cloudEvent.getId());

        // TODO: Implement business logic
        // - Update order read model
        // - Trigger service provisioning
        // - Send order confirmation
        // - Update customer order history

        // Evict order caches
        cacheEvictionService.evictAllOrderCaches();

        log.debug("Order created event processed successfully");
    }

    private void handleOrderUpdated(CloudEvent cloudEvent) {
        log.info("Handling order updated event: {}", cloudEvent.getId());

        // TODO: Implement business logic
        // - Update order read model
        // - Update service provisioning if needed
        // - Send notification

        // Evict order caches
        cacheEvictionService.evictAllOrderCaches();

        log.debug("Order updated event processed successfully");
    }

    private void handleOrderStatusChanged(CloudEvent cloudEvent) {
        log.info("Handling order status changed event: {}", cloudEvent.getId());

        // TODO: Implement business logic
        // - Update order status in read model
        // - Handle status-specific actions:
        //   * APPROVED: Trigger provisioning
        //   * IN_PROGRESS: Update UI
        //   * REJECTED: Send rejection notification
        // - Update customer notification preferences

        log.debug("Order status changed event processed successfully");
    }

    private void handleOrderCompleted(CloudEvent cloudEvent) {
        log.info("Handling order completed event: {}", cloudEvent.getId());

        // TODO: Implement business logic
        // - Update order to COMPLETED status
        // - Activate services
        // - Generate activation confirmation
        // - Send completion notification
        // - Update customer service list
        // - Trigger billing if needed

        // Evict order caches
        cacheEvictionService.evictAllOrderCaches();

        log.debug("Order completed event processed successfully");
    }

    private void handleOrderCancelled(CloudEvent cloudEvent) {
        log.info("Handling order cancelled event: {}", cloudEvent.getId());

        // TODO: Implement business logic
        // - Update order to CANCELLED status
        // - Cancel any pending provisioning
        // - Send cancellation confirmation
        // - Process refund if payment already made
        // - Update customer notification

        // Evict order caches
        cacheEvictionService.evictAllOrderCaches();

        log.debug("Order cancelled event processed successfully");
    }

    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed order event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    private void handleProcessingFailure(
            String eventId,
            String eventType,
            Throwable throwable,
            Acknowledgment acknowledgment
    ) {
        log.error("Failed to process order event: {} - {}: {}",
                eventType, eventId, throwable.getMessage(), throwable);

        totalEventsFailed.incrementAndGet();
        acknowledgment.acknowledge();
    }

    // Metrics getters
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
}
