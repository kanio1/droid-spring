package com.droid.bss.domain.order.event;

import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for Order-related CloudEvents
 * Publishes events to Kafka topics following CloudEvents v1.0 specification
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish OrderCreatedEvent
     */
    public void publishOrderCreated(OrderEntity order) {
        OrderCreatedEvent event = new OrderCreatedEvent(order);
        publishEvent("order.created", event);
    }

    /**
     * Publish OrderUpdatedEvent
     */
    public void publishOrderUpdated(OrderEntity order) {
        OrderUpdatedEvent event = new OrderUpdatedEvent(order);
        publishEvent("order.updated", event);
    }

    /**
     * Publish OrderStatusChangedEvent
     */
    public void publishOrderStatusChanged(OrderEntity order, OrderStatus previousStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(order, previousStatus);
        publishEvent("order.statusChanged", event);
    }

    /**
     * Publish OrderCompletedEvent
     */
    public void publishOrderCompleted(OrderEntity order) {
        OrderCompletedEvent event = new OrderCompletedEvent(order);
        publishEvent("order.completed", event);
    }

    /**
     * Publish OrderCancelledEvent
     */
    public void publishOrderCancelled(OrderEntity order, String cancellationReason) {
        OrderCancelledEvent event = new OrderCancelledEvent(order, cancellationReason);
        publishEvent("order.cancelled", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, OrderEvent event) {
        try {
            // Create CloudEvent according to v1.0 specification
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(event.getId())
                .withType(event.getType())
                .withSource(URI.create(event.getSource()))
                .withTime(OffsetDateTime.from(event.getTime().atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .withData("application/json", objectMapper.writeValueAsBytes(event))
                .build();

            // Publish to Kafka
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, cloudEvent);

            // Log success
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully: topic={}, eventId={}, partition={}, offset={}",
                        topic,
                        cloudEvent.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                    );
                } else {
                    log.error("Failed to publish event: topic={}, eventId={}, error={}",
                        topic,
                        cloudEvent.getId(),
                        ex.getMessage(),
                        ex
                    );
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event: topic={}, eventId={}, error={}",
                topic,
                event.getId(),
                e.getMessage(),
                e
            );
        }
    }
}
