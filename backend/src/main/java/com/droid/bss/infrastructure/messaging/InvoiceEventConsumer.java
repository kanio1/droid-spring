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
 * Invoice Event Consumer
 *
 * Handles CloudEvents for invoice domain:
 * - com.droid.bss.invoice.created.v1
 * - com.droid.bss.invoice.updated.v1
 * - com.droid.bss.invoice.issued.v1
 * - com.droid.bss.invoice.paid.v1
 * - com.droid.bss.invoice.overdue.v1
 * - com.droid.bss.invoice.cancelled.v1
 */
@Component
public class InvoiceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventConsumer.class);

    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);
    private final CacheEvictionService cacheEvictionService;

    public InvoiceEventConsumer(CacheEvictionService cacheEvictionService) {
        this.cacheEvictionService = cacheEvictionService;
    }

    @KafkaListener(
            topics = {
                    "invoice.created",
                    "invoice.updated",
                    "invoice.issued",
                    "invoice.paid",
                    "invoice.overdue",
                    "invoice.cancelled"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInvoiceEvent(
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
            log.info("Received invoice event: {} from topic: {} partition: {} offset: {}",
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
            log.error("Unexpected error processing invoice event: {} from topic: {} offset: {}",
                    eventType, topic, offset, e);
            totalEventsFailed.incrementAndGet();
        }
    }

    private boolean isDuplicateEvent(String eventId) {
        if (processedEventIds.containsKey(eventId)) {
            return true;
        }
        processedEventIds.put(eventId, System.currentTimeMillis());
        long cutoff = System.currentTimeMillis() - 3_600_000;
        processedEventIds.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        return false;
    }

    private CompletableFuture<Void> processEvent(CloudEvent cloudEvent, String topic) {
        return CompletableFuture.runAsync(() -> {
            String eventType = cloudEvent.getType();
            try {
                switch (eventType) {
                    case "com.droid.bss.invoice.created.v1":
                        handleInvoiceCreated(cloudEvent);
                        break;
                    case "com.droid.bss.invoice.updated.v1":
                        handleInvoiceUpdated(cloudEvent);
                        break;
                    case "com.droid.bss.invoice.issued.v1":
                        handleInvoiceIssued(cloudEvent);
                        break;
                    case "com.droid.bss.invoice.paid.v1":
                        handleInvoicePaid(cloudEvent);
                        break;
                    case "com.droid.bss.invoice.overdue.v1":
                        handleInvoiceOverdue(cloudEvent);
                        break;
                    case "com.droid.bss.invoice.cancelled.v1":
                        handleInvoiceCancelled(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown invoice event type: {}", eventType);
                }
                totalEventsProcessed.incrementAndGet();
            } catch (Exception e) {
                log.error("Error processing invoice event: {} - {}", eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    private void handleInvoiceCreated(CloudEvent cloudEvent) {
        log.info("Handling invoice created event: {}", cloudEvent.getId());
        // TODO: Update read model, send notification

        // Evict invoice caches
        cacheEvictionService.evictAllInvoiceCaches();

        log.debug("Invoice created event processed successfully");
    }

    private void handleInvoiceUpdated(CloudEvent cloudEvent) {
        log.info("Handling invoice updated event: {}", cloudEvent.getId());
        // TODO: Update read model

        // Evict invoice caches
        cacheEvictionService.evictAllInvoiceCaches();

        log.debug("Invoice updated event processed successfully");
    }

    private void handleInvoiceIssued(CloudEvent cloudEvent) {
        log.info("Handling invoice issued event: {}", cloudEvent.getId());
        // TODO: Update status, send to customer, trigger payment reminder
        log.debug("Invoice issued event processed successfully");
    }

    private void handleInvoicePaid(CloudEvent cloudEvent) {
        log.info("Handling invoice paid event: {}", cloudEvent.getId());
        // TODO: Update status, send confirmation, update customer balance

        // Evict invoice caches
        cacheEvictionService.evictAllInvoiceCaches();

        log.debug("Invoice paid event processed successfully");
    }

    private void handleInvoiceOverdue(CloudEvent cloudEvent) {
        log.info("Handling invoice overdue event: {}", cloudEvent.getId());
        // TODO: Send overdue notice, apply late fees, suspend services

        // Evict invoice caches
        cacheEvictionService.evictAllInvoiceCaches();

        log.debug("Invoice overdue event processed successfully");
    }

    private void handleInvoiceCancelled(CloudEvent cloudEvent) {
        log.info("Handling invoice cancelled event: {}", cloudEvent.getId());
        // TODO: Update status, send cancellation notice, adjust customer balance

        // Evict invoice caches
        cacheEvictionService.evictAllInvoiceCaches();

        log.debug("Invoice cancelled event processed successfully");
    }

    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed invoice event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    private void handleProcessingFailure(String eventId, String eventType, Throwable throwable, Acknowledgment acknowledgment) {
        log.error("Failed to process invoice event: {} - {}: {}", eventType, eventId, throwable.getMessage(), throwable);
        totalEventsFailed.incrementAndGet();
        acknowledgment.acknowledge();
    }

    // Metrics
    public long getTotalEventsProcessed() { return totalEventsProcessed.get(); }
    public long getTotalEventsFailed() { return totalEventsFailed.get(); }
    public double getSuccessRate() {
        long total = totalEventsProcessed.get() + totalEventsFailed.get();
        return total == 0 ? 0.0 : (double) totalEventsProcessed.get() / total * 100.0;
    }
}
