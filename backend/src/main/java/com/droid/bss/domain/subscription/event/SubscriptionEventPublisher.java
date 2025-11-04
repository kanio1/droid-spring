package com.droid.bss.domain.subscription.event;

import com.droid.bss.domain.subscription.SubscriptionEntity;
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
 * Publisher for Subscription-related CloudEvents
 * Publishes events to Kafka topics following CloudEvents v1.0 specification
 */
@Component
public class SubscriptionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SubscriptionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish SubscriptionCreatedEvent
     */
    public void publishSubscriptionCreated(SubscriptionEntity subscription) {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(subscription);
        publishEvent("subscription.created", event);
    }

    /**
     * Publish SubscriptionRenewedEvent
     */
    public void publishSubscriptionRenewed(SubscriptionEntity subscription) {
        SubscriptionRenewedEvent event = new SubscriptionRenewedEvent(subscription);
        publishEvent("subscription.renewed", event);
    }

    /**
     * Publish SubscriptionSuspendedEvent
     */
    public void publishSubscriptionSuspended(SubscriptionEntity subscription, String suspensionReason) {
        SubscriptionSuspendedEvent event = new SubscriptionSuspendedEvent(subscription, suspensionReason);
        publishEvent("subscription.suspended", event);
    }

    /**
     * Publish SubscriptionCancelledEvent
     */
    public void publishSubscriptionCancelled(SubscriptionEntity subscription, String cancellationReason) {
        SubscriptionCancelledEvent event = new SubscriptionCancelledEvent(subscription, cancellationReason);
        publishEvent("subscription.cancelled", event);
    }

    /**
     * Publish SubscriptionExpiredEvent
     */
    public void publishSubscriptionExpired(SubscriptionEntity subscription) {
        SubscriptionExpiredEvent event = new SubscriptionExpiredEvent(subscription);
        publishEvent("subscription.expired", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, SubscriptionEvent event) {
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
