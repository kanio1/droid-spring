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
 * Payment Event Consumer
 *
 * Handles CloudEvents for payment domain:
 * - com.droid.bss.payment.created.v1
 * - com.droid.bss.payment.processing.v1
 * - com.droid.bss.payment.completed.v1
 * - com.droid.bss.payment.failed.v1
 * - com.droid.bss.payment.refunded.v1
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ConcurrentMap<String, Long> processedEventIds = new ConcurrentHashMap<>();
    private final AtomicLong totalEventsProcessed = new AtomicLong(0);
    private final AtomicLong totalEventsFailed = new AtomicLong(0);

    @KafkaListener(
            topics = {
                    "payment.created",
                    "payment.processing",
                    "payment.completed",
                    "payment.failed",
                    "payment.refunded"
            },
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-backend}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
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
            log.info("Received payment event: {} from topic: {} partition: {} offset: {}",
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
            log.error("Unexpected error processing payment event: {} from topic: {} offset: {}",
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
                    case "com.droid.bss.payment.created.v1":
                        handlePaymentCreated(cloudEvent);
                        break;
                    case "com.droid.bss.payment.processing.v1":
                        handlePaymentProcessing(cloudEvent);
                        break;
                    case "com.droid.bss.payment.completed.v1":
                        handlePaymentCompleted(cloudEvent);
                        break;
                    case "com.droid.bss.payment.failed.v1":
                        handlePaymentFailed(cloudEvent);
                        break;
                    case "com.droid.bss.payment.refunded.v1":
                        handlePaymentRefunded(cloudEvent);
                        break;
                    default:
                        log.warn("Unknown payment event type: {}", eventType);
                }
                totalEventsProcessed.incrementAndGet();
            } catch (Exception e) {
                log.error("Error processing payment event: {} - {}", eventType, e.getMessage(), e);
                throw new RuntimeException("Failed to process event: " + eventType, e);
            }
        });
    }

    private void handlePaymentCreated(CloudEvent cloudEvent) {
        log.info("Handling payment created event: {}", cloudEvent.getId());
        // TODO: Update read model, send confirmation
        log.debug("Payment created event processed successfully");
    }

    private void handlePaymentProcessing(CloudEvent cloudEvent) {
        log.info("Handling payment processing event: {}", cloudEvent.getId());
        // TODO: Update status, send processing notification
        log.debug("Payment processing event processed successfully");
    }

    private void handlePaymentCompleted(CloudEvent cloudEvent) {
        log.info("Handling payment completed event: {}", cloudEvent.getId());
        // TODO: Update status, mark invoice as paid, send receipt
        log.debug("Payment completed event processed successfully");
    }

    private void handlePaymentFailed(CloudEvent cloudEvent) {
        log.info("Handling payment failed event: {}", cloudEvent.getId());
        // TODO: Update status, send failure notification, retry if needed
        log.debug("Payment failed event processed successfully");
    }

    private void handlePaymentRefunded(CloudEvent cloudEvent) {
        log.info("Handling payment refunded event: {}", cloudEvent.getId());
        // TODO: Update status, adjust invoice, send refund confirmation
        log.debug("Payment refunded event processed successfully");
    }

    private void handleProcessingSuccess(String eventId, String eventType, Acknowledgment acknowledgment) {
        log.info("Successfully processed payment event: {} - {}", eventType, eventId);
        acknowledgment.acknowledge();
    }

    private void handleProcessingFailure(String eventId, String eventType, Throwable throwable, Acknowledgment acknowledgment) {
        log.error("Failed to process payment event: {} - {}: {}", eventType, eventId, throwable.getMessage(), throwable);
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
