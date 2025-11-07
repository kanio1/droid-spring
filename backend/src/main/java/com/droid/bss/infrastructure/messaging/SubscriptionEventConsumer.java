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
 * Subscription Event Consumer
 *
 * Handles CloudEvents for subscription domain:
 * - com.droid.bss.subscription.created.v1
 * - com.droid.bss.subscription.activated.v1
 * - com.droid.bss.subscription.updated.v1
 * - com.droid.bss.subscription.suspended.v1
 * - com.droid.bss.subscription.cancelled.v1
 * - com.droid.bss.subscription.renewed.v1
 */
@Component
public class SubscriptionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);

    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);

    @KafkaListener(
            topics = {
                    "subscription.created",
                    "subscription.activated",
                    "subscription.updated",
                    "subscription.suspended",
                    "subscription.cancelled",
                    "subscription.renewed"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleSubscriptionEvent(
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
            log.info("Received subscription event: {} from topic: {} partition: {} offset: {}",
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
            log.error("Unexpected error processing subscription event: {} from topic: {} offset: {}",
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
                    case "com.droid.bss.subscription.created.v1":
                        handleSubscriptionCreated(cloudEvent);
                        break;
                    case "com.droid.bss.subscription.activated.v1":
                        handleSubscriptionActivated(cloudEvent);
                        break;
                    case "com.droid.bss.subscription.updated.v1":
                        handleSubscriptionUpdated(cloudEvent);
                        break;
                    case "com.droid.bss.subscription.suspended.v1":
                        handleSubscriptionSuspended(cloudEvent);
                        break;
                    case "com.droid.bss.subscription.cancelled.v1":
                        handleSubscriptionCancelled(cloudEvent);
                        break;
                    case "com.droid.bss.subscription.renewed.v1":
                        handleSubscriptionRenewed(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown subscription event type: {}", eventType);
                }
                totalEventsProcessed.incrementAndGet();
            } catch (Exception e) {
                log.error("Error processing subscription event: {} - {}", eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    private void handleSubscriptionCreated(CloudEvent cloudEvent) {
        log.info("Handling subscription created event: {}", cloudEvent.getId());
        // TODO: Update read model, send confirmation
        log.debug("Subscription created event processed successfully");
    }

    private void handleSubscriptionActivated(CloudEvent cloudEvent) {
        log.info("Handling subscription activated event: {}", cloudEvent.getId());
        // TODO: Update status, activate services, send activation confirmation
        log.debug("Subscription activated event processed successfully");
    }

    private void handleSubscriptionUpdated(CloudEvent cloudEvent) {
        log.info("Handling subscription updated event: {}", cloudEvent.getId());
        // TODO: Update read model, adjust billing if needed
        log.debug("Subscription updated event processed successfully");
    }

    private void handleSubscriptionSuspended(CloudEvent cloudEvent) {
        log.info("Handling subscription suspended event: {}", cloudEvent.getId());
        // TODO: Suspend services, send suspension notice
        log.debug("Subscription suspended event processed successfully");
    }

    private void handleSubscriptionCancelled(CloudEvent cloudEvent) {
        log.info("Handling subscription cancelled event: {}", cloudEvent.getId());
        // TODO: Cancel services, send cancellation confirmation
        log.debug("Subscription cancelled event processed successfully");
    }

    private void handleSubscriptionRenewed(CloudEvent cloudEvent) {
        log.info("Handling subscription renewed event: {}", cloudEvent.getId());
        // TODO: Update renewal date, send renewal confirmation
        log.debug("Subscription renewed event processed successfully");
    }

    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed subscription event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    private void handleProcessingFailure(String eventId, String eventType, Throwable throwable, Acknowledgment acknowledgment) {
        log.error("Failed to process subscription event: {} - {}: {}", eventType, eventId, throwable.getMessage(), throwable);
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
