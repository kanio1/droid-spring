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
 * Consumer for Order-related CloudEvents
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    private static final String TOPIC = "order.events";
    private static final Set<String> processedEventIds = new HashSet<>();

    @KafkaListener(topics = TOPIC, groupId = "${spring.kafka.consumer.group-id:bss-backend}")
    public void handleOrderEvent(
            @Payload CloudEvent event,
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = event.getId();

        try {
            log.info("Received order event: topic={}, partition={}, offset={}, eventId={}, type={}",
                record.topic(), record.partition(), record.offset(), eventId, event.getType());

            if (processedEventIds.contains(eventId)) {
                log.warn("Duplicate event detected, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }

            String eventType = event.getType();
            processEventByType(event, eventType);

            processedEventIds.add(eventId);
            log.info("Successfully processed order event: eventId={}, type={}", eventId, eventType);

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing order event: eventId={}, topic={}, partition={}, offset={}, error={}",
                eventId, record.topic(), record.partition(), record.offset(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void processEventByType(CloudEvent event, String eventType) {
        log.debug("Processing order event type: {}", eventType);

        switch (eventType) {
            case "com.droid.bss.order.created.v1":
                handleOrderCreated(event);
                break;
            case "com.droid.bss.order.updated.v1":
                handleOrderUpdated(event);
                break;
            case "com.droid.bss.order.statusChanged.v1":
                handleOrderStatusChanged(event);
                break;
            case "com.droid.bss.order.completed.v1":
                handleOrderCompleted(event);
                break;
            case "com.droid.bss.order.cancelled.v1":
                handleOrderCancelled(event);
                break;
            default:
                log.warn("Unknown order event type: {}", eventType);
        }
    }

    private void handleOrderCreated(CloudEvent event) {
        log.info("Handling order created event: {}", event.getId());
    }

    private void handleOrderUpdated(CloudEvent event) {
        log.info("Handling order updated event: {}", event.getId());
    }

    private void handleOrderStatusChanged(CloudEvent event) {
        log.info("Handling order status changed event: {}", event.getId());
    }

    private void handleOrderCompleted(CloudEvent event) {
        log.info("Handling order completed event: {}", event.getId());
    }

    private void handleOrderCancelled(CloudEvent event) {
        log.info("Handling order cancelled event: {}", event.getId());
    }
}
