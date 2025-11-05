package com.droid.bss.domain.service.event;

import com.droid.bss.domain.service.*;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher for Service-related CloudEvents
 * Publishes events to Kafka topics following CloudEvents v1.0 specification
 */
@Component
public class ServiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ServiceEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // No-args constructor for tests
    public ServiceEventPublisher() {
        this.kafkaTemplate = null;
        this.objectMapper = null;
    }

    public ServiceEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish ServiceCreatedEvent
     */
    public void publishServiceCreated(ServiceEntity service) {
        ServiceCreatedEvent event = new ServiceCreatedEvent(service);
        publishEvent("service.created", event);
    }

    /**
     * Publish ServiceUpdatedEvent
     */
    public void publishServiceUpdated(ServiceEntity service) {
        ServiceUpdatedEvent event = new ServiceUpdatedEvent(service);
        publishEvent("service.updated", event);
    }

    /**
     * Publish ServiceActivatedEvent
     */
    public void publishServiceActivated(ServiceEntity service, ServiceActivationEntity activation) {
        ServiceActivatedEvent event = new ServiceActivatedEvent(service, activation);
        publishEvent("service.activated", event);
    }

    /**
     * Publish ServiceActivationCompletedEvent
     */
    public void publishServiceActivationCompleted(ServiceEntity service, ServiceActivationEntity activation) {
        ServiceActivationCompletedEvent event = new ServiceActivationCompletedEvent(service, activation);
        publishEvent("service.activation.completed", event);
    }

    /**
     * Publish ServiceActivationFailedEvent
     */
    public void publishServiceActivationFailed(ServiceEntity service, ServiceActivationEntity activation, String errorMessage) {
        ServiceActivationFailedEvent event = new ServiceActivationFailedEvent(service, activation, errorMessage);
        publishEvent("service.activation.failed", event);
    }

    /**
     * Publish ServiceDeactivatedEvent
     */
    public void publishServiceDeactivated(ServiceEntity service, UUID customerId, String reason) {
        ServiceDeactivatedEvent event = new ServiceDeactivatedEvent(service, customerId, reason);
        publishEvent("service.deactivated", event);
    }

    /**
     * Publish ServiceDeactivationCompletedEvent
     */
    public void publishServiceDeactivationCompleted(ServiceEntity service, UUID customerId, UUID deactivationId) {
        ServiceDeactivationCompletedEvent event = new ServiceDeactivationCompletedEvent(service, customerId, deactivationId);
        publishEvent("service.deactivation.completed", event);
    }

    /**
     * Publish ServiceActivationStatusChangedEvent
     */
    public void publishServiceActivationStatusChanged(
            ServiceEntity service,
            ServiceActivationEntity activation,
            ActivationStatus previousStatus) {
        ServiceActivationStatusChangedEvent event = new ServiceActivationStatusChangedEvent(service, activation, previousStatus);
        publishEvent("service.activation.statusChanged", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, ServiceEvent event) {
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
