package com.droid.bss.domain.customer.event;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
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
 * Publisher for Customer-related CloudEvents
 * Publishes events to Kafka topics following CloudEvents v1.0 specification
 */
@Component
public class CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // No-args constructor for tests
    public CustomerEventPublisher() {
        this.kafkaTemplate = null;
        this.objectMapper = null;
    }

    public CustomerEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish CustomerCreatedEvent
     */
    public void publishCustomerCreated(CustomerEntity customer) {
        CustomerCreatedEvent event = new CustomerCreatedEvent(customer);
        publishEvent("customer.created", event);
    }

    /**
     * Publish CustomerUpdatedEvent
     */
    public void publishCustomerUpdated(CustomerEntity customer) {
        CustomerUpdatedEvent event = new CustomerUpdatedEvent(customer);
        publishEvent("customer.updated", event);
    }

    /**
     * Publish CustomerStatusChangedEvent
     */
    public void publishCustomerStatusChanged(CustomerEntity customer, CustomerStatus previousStatus) {
        CustomerStatusChangedEvent event = new CustomerStatusChangedEvent(customer, previousStatus);
        publishEvent("customer.statusChanged", event);
    }

    /**
     * Publish CustomerTerminatedEvent
     */
    public void publishCustomerTerminated(CustomerEntity customer, String terminationReason) {
        CustomerTerminatedEvent event = new CustomerTerminatedEvent(customer, terminationReason);
        publishEvent("customer.terminated", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, CustomerEvent event) {
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
