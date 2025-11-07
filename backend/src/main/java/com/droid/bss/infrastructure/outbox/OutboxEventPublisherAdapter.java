package com.droid.bss.infrastructure.outbox;

import com.droid.bss.domain.outbox.OutboxEventType;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox Event Publisher Adapter
 *
 * Integrates existing CloudEvents with the Outbox Pattern
 * This allows gradual migration from direct Kafka publishing to outbox pattern
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherAdapter {

    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String defaultTopic = "bss.events";

    /**
     * Publish CloudEvent via Outbox Pattern
     */
    public void publishEventViaOutbox(
            OutboxEventType outboxType,
            String eventName,
            String aggregateId,
            String aggregateType,
            CloudEvent cloudEvent,
            String correlationId,
            String causationId,
            String userId) {

        try {
            // Convert CloudEvent to event data
            EventData eventData = EventData.builder()
                    .cloudEvent(cloudEvent)
                    .build();

            // Publish via outbox
            outboxEventPublisher.publishEvent(
                    outboxType,
                    eventName,
                    aggregateId,
                    aggregateType,
                    eventData,
                    correlationId,
                    causationId,
                    userId
            );

            log.debug("Event published to outbox: {} (ID: {})", eventName, cloudEvent.getId());

        } catch (Exception e) {
            log.error("Failed to publish event to outbox: {} (ID: {})", eventName, cloudEvent.getId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * Publish CloudEvent directly to Kafka (legacy method)
     * Kept for backward compatibility and gradual migration
     */
    @Deprecated
    public void publishEventDirectly(String topic, CloudEvent event) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Event published directly to Kafka: topic={}, eventId={}",
                    topic, event.getId());
            } else {
                log.error("Failed to publish event directly to Kafka: topic={}, eventId={}, error={}",
                    topic, event.getId(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Create a CloudEvent from existing event
     */
    public CloudEvent createCloudEvent(
            String eventId,
            String eventType,
            String source,
            String aggregateId,
            Object eventData) {

        return CloudEventBuilder.v1()
                .withId(eventId != null ? eventId : UUID.randomUUID().toString())
                .withType(eventType)
                .withSource(URI.create(source))
                .withTime(OffsetDateTime.now())
                .withData("application/json", serializeEventData(eventData))
                .build();
    }

    /**
     * Event data wrapper for CloudEvents
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EventData {
        private CloudEvent cloudEvent;
    }

    /**
     * Serialize event data to JSON bytes
     */
    private byte[] serializeEventData(Object data) {
        try {
            return new ObjectMapper().writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }
}
