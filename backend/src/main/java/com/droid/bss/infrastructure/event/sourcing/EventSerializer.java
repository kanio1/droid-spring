package com.droid.bss.infrastructure.event.sourcing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Event serializer for converting between domain events and stored events
 */
@Component
public class EventSerializer {

    private final ObjectMapper objectMapper;

    public EventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize a domain event to JSON string
     */
    public String serializeEvent(CloudEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    /**
     * Deserialize JSON string to CloudEvent
     */
    public CloudEvent deserializeEvent(String eventData) {
        try {
            return objectMapper.readValue(eventData, CloudEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    /**
     * Build a CloudEvent from stored event
     */
    public CloudEvent toCloudEvent(StoredEvent storedEvent) {
        return CloudEventBuilder.v1()
                .withId(storedEvent.getId().toString())
                .withType(storedEvent.getEventType())
                .withSource(URI.create("urn:droid:bss:event-sourcing"))
                .withTime(OffsetDateTime.from(storedEvent.getTimestamp().atZone(java.time.ZoneId.systemDefault())))
                .withData("application/json", storedEvent.getEventData().getBytes())
                .build();
    }

    /**
     * Create a StoredEvent from CloudEvent
     */
    public StoredEvent fromCloudEvent(
            CloudEvent cloudEvent,
            String aggregateId,
            String aggregateType,
            String userId,
            String correlationId,
            long version) {
        String eventData = new String(cloudEvent.getData().toBytes());
        return StoredEvent.fromDomainEvent(
                UUID.fromString(cloudEvent.getId()),
                aggregateId,
                aggregateType,
                cloudEvent.getType(),
                eventData,
                userId,
                correlationId,
                version
        );
    }

    /**
     * Extract aggregate ID from CloudEvent extension
     */
    public String getAggregateIdFromEvent(CloudEvent event) {
        return event.getExtension("aggregateId").toString();
    }

    /**
     * Extract correlation ID from CloudEvent extension
     */
    public String getCorrelationIdFromEvent(CloudEvent event) {
        return event.getExtension("correlationId").toString();
    }

    /**
     * Get event type from CloudEvent
     */
    public String getEventType(CloudEvent event) {
        return event.getType();
    }
}
