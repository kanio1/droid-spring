package com.droid.bss.infrastructure.messaging;

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
 * Service Event Consumer
 *
 * Handles CloudEvents for service domain:
 * - com.droid.bss.service.activated.v1
 * - com.droid.bss.service.deactivated.v1
 * - com.droid.bss.service.provisioned.v1
 * - com.droid.bss.service.failed.v1
 */
@Component
public class ServiceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ServiceEventConsumer.class);

    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);

    @KafkaListener(
            topics = {
                    "service.activated",
                    "service.deactivated",
                    "service.provisioned",
                    "service.failed"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleServiceEvent(
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
            log.info("Received service event: {} from topic: {} partition: {} offset: {}",
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
            log.error("Unexpected error processing service event: {} from topic: {} offset: {}",
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
                    case "com.droid.bss.service.activated.v1":
                        handleServiceActivated(cloudEvent);
                        break;
                    case "com.droid.bss.service.deactivated.v1":
                        handleServiceDeactivated(cloudEvent);
                        break;
                    case "com.droid.bss.service.provisioned.v1":
                        handleServiceProvisioned(cloudEvent);
                        break;
                    case "com.droid.bss.service.failed.v1":
                        handleServiceFailed(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown service event type: {}", eventType);
                }
                totalEventsProcessed.incrementAndGet();
            } catch (Exception e) {
                log.error("Error processing service event: {} - {}", eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    private void handleServiceActivated(CloudEvent cloudEvent) {
        log.info("Handling service activated event: {}", cloudEvent.getId());
        // TODO: Update read model, send activation confirmation
        log.debug("Service activated event processed successfully");
    }

    private void handleServiceDeactivated(CloudEvent cloudEvent) {
        log.info("Handling service deactivated event: {}", cloudEvent.getId());
        // TODO: Update status, send deactivation notice
        log.debug("Service deactivated event processed successfully");
    }

    private void handleServiceProvisioned(CloudEvent cloudEvent) {
        log.info("Handling service provisioned event: {}", cloudEvent.getId());
        // TODO: Update provisioning status, notify customer
        log.debug("Service provisioned event processed successfully");
    }

    private void handleServiceFailed(CloudEvent cloudEvent) {
        log.info("Handling service failed event: {}", cloudEvent.getId());
        // TODO: Update failure status, send failure notification, retry if needed
        log.debug("Service failed event processed successfully");
    }

    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed service event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    private void handleProcessingFailure(String eventId, String eventType, Throwable throwable, Acknowledgment acknowledgment) {
        log.error("Failed to process service event: {} - {}: {}", eventType, eventId, throwable.getMessage(), throwable);
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
