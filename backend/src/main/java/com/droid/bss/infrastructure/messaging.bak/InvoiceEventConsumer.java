package com.droid.bss.infrastructure.messaging;

import com.droid.bss.domain.invoice.event.InvoiceEvent;
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
import java.util.concurrent.TimeUnit;

/**
 * Consumer for Invoice-related CloudEvents
 * Implements idempotency and error handling
 */
@Component
public class InvoiceEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventConsumer.class);
    private static final String TOPIC = "invoice.events";
    private static final Set<String> processedEventIds = new HashSet<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id:bss-backend}")
    public void handleInvoiceEvent(
            @Payload CloudEvent event,
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getId();

        try {
            log.info("Received invoice event: topic={}, partition={}, offset={}, eventId={}, type={}",
                record.topic(), record.partition(), record.offset(), eventId, event.getType());

            // Check if event was already processed (idempotency)
            if (processedEventIds.contains(eventId)) {
                log.warn("Duplicate event detected, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            // Process event based on type
            String eventType = event.getType();
            processEventByType(event, eventType);

            // Mark event as processed
            processedEventIds.add(eventId);
            log.info("Successfully processed invoice event: eventId={}, type={}", eventId, eventType);

            // Acknowledge successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing invoice event: eventId={}, topic={}, partition={}, offset={}, error={}",
                eventId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);

            // Handle error - could implement retry logic here
            // For now, acknowledge to avoid infinite retries
            // In production, you might want to send to DLQ (Dead Letter Queue)
            acknowledgment.acknowledge();

            // TODO: Implement retry logic with exponential backoff
            // TODO: Send failed events to DLQ topic
        }
    }

    private void processEventByType(CloudEvent event, String eventType) {
        log.debug("Processing invoice event type: {}", eventType);

        switch (eventType) {
            case "com.droid.bss.invoice.created.v1":
                handleInvoiceCreated(event);
                break;
            case "com.droid.bss.invoice.updated.v1":
                handleInvoiceUpdated(event);
                break;
            case "com.droid.bss.invoice.statusChanged.v1":
                handleInvoiceStatusChanged(event);
                break;
            case "com.droid.bss.invoice.sent.v1":
                handleInvoiceSent(event);
                break;
            case "com.droid.bss.invoice.paid.v1":
                handleInvoicePaid(event);
                break;
            default:
                log.warn("Unknown invoice event type: {}", eventType);
        }
    }

    private void handleInvoiceCreated(CloudEvent event) {
        log.info("Handling invoice created event: {}", event.getId());
        // TODO: Implement business logic for invoice creation
        // e.g., update search indexes, notify billing system, etc.
    }

    private void handleInvoiceUpdated(CloudEvent event) {
        log.info("Handling invoice updated event: {}", event.getId());
        // TODO: Implement business logic for invoice update
    }

    private void handleInvoiceStatusChanged(CloudEvent event) {
        log.info("Handling invoice status changed event: {}", event.getId());
        // TODO: Implement business logic for status change
    }

    private void handleInvoiceSent(CloudEvent event) {
        log.info("Handling invoice sent event: {}", event.getId());
        // TODO: Implement business logic for invoice sent
    }

    private void handleInvoicePaid(CloudEvent event) {
        log.info("Handling invoice paid event: {}", event.getId());
        // TODO: Implement business logic for invoice paid
    }
}
