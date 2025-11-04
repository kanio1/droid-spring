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
 * Consumer for Customer-related CloudEvents
 */
@Component
public class CustomerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventConsumer.class);
    private static final String TOPIC = "customer.events";
    private static final Set<String> processedEventIds = new HashSet<>();

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id:bss-backend}")
    public void handleCustomerEvent(
            @Payload CloudEvent event,
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getId();

        try {
            log.info("Received customer event: topic={}, partition={}, offset={}, eventId={}, type={}",
                record.topic(), record.partition(), record.offset(), eventId, event.getType());

            if (processedEventIds.contains(eventId)) {
                log.warn("Duplicate event detected, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            String eventType = event.getType();
            processEventByType(event, eventType);

            processedEventIds.add(eventId);
            log.info("Successfully processed customer event: eventId={}, type={}", eventId, eventType);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing customer event: eventId={}, topic={}, partition={}, offset={}, error={}",
                eventId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void processEventByType(CloudEvent event, String eventType) {
        log.debug("Processing customer event type: {}", eventType);

        switch (eventType) {
            case "com.droid.bss.customer.created.v1":
                handleCustomerCreated(event);
                break;
            case "com.droid.bss.customer.updated.v1":
                handleCustomerUpdated(event);
                break;
            case "com.droid.bss.customer.statusChanged.v1":
                handleCustomerStatusChanged(event);
                break;
            case "com.droid.bss.customer.terminated.v1":
                handleCustomerTerminated(event);
                break;
            default:
                log.warn("Unknown customer event type: {}", eventType);
        }
    }

    private void handleCustomerCreated(CloudEvent event) {
        log.info("Handling customer created event: {}", event.getId());
    }

    private void handleCustomerUpdated(CloudEvent event) {
        log.info("Handling customer updated event: {}", event.getId());
    }

    private void handleCustomerStatusChanged(CloudEvent event) {
        log.info("Handling customer status changed event: {}", event.getId());
    }

    private void handleCustomerTerminated(CloudEvent event) {
        log.info("Handling customer terminated event: {}", event.getId());
    }
}
