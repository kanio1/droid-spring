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
 * Consumer for Subscription-related CloudEvents
 */
@Component
public class SubscriptionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventConsumer.class);
    private static final String TOPIC = "subscription.events";
    private static final Set<String> processedEventIds = new HashSet<>();

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id:bss-backend}")
    public void handleSubscriptionEvent(
            @Payload CloudEvent event,
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getId();

        try {
            log.info("Received subscription event: topic={}, partition={}, offset={}, eventId={}, type={}",
                record.topic(), record.partition(), record.offset(), eventId, event.getType());

            if (processedEventIds.contains(eventId)) {
                log.warn("Duplicate event detected, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            String eventType = event.getType();
            processEventByType(event, eventType);

            processedEventIds.add(eventId);
            log.info("Successfully processed subscription event: eventId={}, type={}", eventId, eventType);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing subscription event: eventId={}, topic={}, partition={}, offset={}, error={}",
                eventId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void processEventByType(CloudEvent event, String eventType) {
        log.debug("Processing subscription event type: {}", eventType);

        switch (eventType) {
            case "com.droid.bss.subscription.created.v1":
                handleSubscriptionCreated(event);
                break;
            case "com.droid.bss.subscription.renewed.v1":
                handleSubscriptionRenewed(event);
                break;
            case "com.droid.bss.subscription.suspended.v1":
                handleSubscriptionSuspended(event);
                break;
            case "com.droid.bss.subscription.cancelled.v1":
                handleSubscriptionCancelled(event);
                break;
            case "com.droid.bss.subscription.expired.v1":
                handleSubscriptionExpired(event);
                break;
            default:
                log.warn("Unknown subscription event type: {}", eventType);
        }
    }

    private void handleSubscriptionCreated(CloudEvent event) {
        log.info("Handling subscription created event: {}", event.getId());
    }

    private void handleSubscriptionRenewed(CloudEvent event) {
        log.info("Handling subscription renewed event: {}", event.getId());
    }

    private void handleSubscriptionSuspended(CloudEvent event) {
        log.info("Handling subscription suspended event: {}", event.getId());
    }

    private void handleSubscriptionCancelled(CloudEvent event) {
        log.info("Handling subscription cancelled event: {}", event.getId());
    }

    private void handleSubscriptionExpired(CloudEvent event) {
        log.info("Handling subscription expired event: {}", event.getId());
    }
}
