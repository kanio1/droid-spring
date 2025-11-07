package com.droid.bss.infrastructure.event.publisher;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Production-ready Kafka Event Publisher
 * Implements CloudEvents v1.0 specification for event publishing to Kafka
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish an event to Kafka topic using CloudEvents format
     *
     * @param topic Kafka topic name
     * @param eventId Unique event identifier
     * @param eventType Event type (e.g., com.droid.bss.customer.created.v1)
     * @param source Event source URN
     * @param data Event data payload
     */
    public void publishEvent(String topic, String eventId, String eventType, String source, Object data) {
        try {
            // Create CloudEvent according to v1.0 specification
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(eventId)
                .withType(eventType)
                .withSource(URI.create(source))
                .withTime(OffsetDateTime.now())
                .withData("application/json", serialize(data))
                .build();

            // Publish to Kafka
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, eventId, cloudEvent);

            // Log success/failure
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully: topic={}, eventId={}, eventType={}, partition={}, offset={}",
                        topic,
                        cloudEvent.getId(),
                        cloudEvent.getType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                    );
                } else {
                    log.error("Failed to publish event: topic={}, eventId={}, eventType={}, error={}",
                        topic,
                        cloudEvent.getId(),
                        cloudEvent.getType(),
                        ex.getMessage(),
                        ex
                    );
                }
            });

        } catch (Exception e) {
            log.error("Error preparing event for publication: topic={}, eventId={}, error={}",
                topic, eventId, e.getMessage(), e);
            throw new EventPublishingException("Failed to publish event", e);
        }
    }

    /**
     * Publish a domain event with automatic CloudEvent wrapping
     *
     * @param topic Kafka topic name
     * @param event Domain event object
     * @param eventType Event type
     * @param source Event source
     */
    public void publishDomainEvent(String topic, Object event, String eventType, String source) {
        String eventId = generateEventId();
        publishEvent(topic, eventId, eventType, source, event);
    }

    /**
     * Generate a unique event ID
     */
    private String generateEventId() {
        return "evt-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * Serialize object to JSON bytes
     */
    private byte[] serialize(Object data) {
        try {
            if (data == null) {
                return new byte[0];
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("Failed to serialize event data", e);
            throw new EventPublishingException("Serialization failed", e);
        }
    }

    /**
     * Exception for event publishing errors
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
