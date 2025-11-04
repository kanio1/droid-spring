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

import java.util.HashSet;
import java.util.Set;

/**
 * Consumer for Payment-related CloudEvents
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private static final String TOPIC = "payment.events";
    private static final Set<String> processedEventIds = new HashSet<>();

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id:bss-backend}")
    public void handlePaymentEvent(
            @Payload CloudEvent event,
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getId();

        try {
            log.info("Received payment event: topic={}, partition={}, offset={}, eventId={}, type={}",
                record.topic(), record.partition(), record.offset(), eventId, event.getType());

            if (processedEventIds.contains(eventId)) {
                log.warn("Duplicate event detected, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            String eventType = event.getType();
            processEventByType(event, eventType);

            processedEventIds.add(eventId);
            log.info("Successfully processed payment event: eventId={}, type={}", eventId, eventType);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing payment event: eventId={}, topic={}, partition={}, offset={}, error={}",
                eventId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void processEventByType(CloudEvent event, String eventType) {
        log.debug("Processing payment event type: {}", eventType);

        switch (eventType) {
            case "com.droid.bss.payment.created.v1":
                handlePaymentCreated(event);
                break;
            case "com.droid.bss.payment.processing.v1":
                handlePaymentProcessing(event);
                break;
            case "com.droid.bss.payment.completed.v1":
                handlePaymentCompleted(event);
                break;
            case "com.droid.bss.payment.failed.v1":
                handlePaymentFailed(event);
                break;
            case "com.droid.bss.payment.refunded.v1":
                handlePaymentRefunded(event);
                break;
            default:
                log.warn("Unknown payment event type: {}", eventType);
        }
    }

    private void handlePaymentCreated(CloudEvent event) {
        log.info("Handling payment created event: {}", event.getId());
    }

    private void handlePaymentProcessing(CloudEvent event) {
        log.info("Handling payment processing event: {}", event.getId());
    }

    private void handlePaymentCompleted(CloudEvent event) {
        log.info("Handling payment completed event: {}", event.getId());
    }

    private void handlePaymentFailed(CloudEvent event) {
        log.info("Handling payment failed event: {}", event.getId());
    }

    private void handlePaymentRefunded(CloudEvent event) {
        log.info("Handling payment refunded event: {}", event.getId());
    }
}
