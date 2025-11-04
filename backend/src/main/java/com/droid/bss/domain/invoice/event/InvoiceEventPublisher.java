package com.droid.bss.domain.invoice.event;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;
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
 * Publisher for Invoice-related CloudEvents
 * Currently logs events to console - ready for Kafka integration
 */
@Component
public class InvoiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InvoiceEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish InvoiceCreatedEvent
     */
    public void publishInvoiceCreated(InvoiceEntity invoice) {
        InvoiceCreatedEvent event = new InvoiceCreatedEvent(invoice);
        publishEvent("invoice.created", event);
    }

    /**
     * Publish InvoiceUpdatedEvent
     */
    public void publishInvoiceUpdated(InvoiceEntity invoice) {
        InvoiceUpdatedEvent event = new InvoiceUpdatedEvent(invoice);
        publishEvent("invoice.updated", event);
    }

    /**
     * Publish InvoiceStatusChangedEvent
     */
    public void publishInvoiceStatusChanged(InvoiceEntity invoice, InvoiceStatus previousStatus) {
        InvoiceStatusChangedEvent event = new InvoiceStatusChangedEvent(invoice, previousStatus);
        publishEvent("invoice.statusChanged", event);
    }

    /**
     * Publish InvoiceSentEvent
     */
    public void publishInvoiceSent(InvoiceEntity invoice) {
        InvoiceSentEvent event = new InvoiceSentEvent(invoice);
        publishEvent("invoice.sent", event);
    }

    /**
     * Publish InvoicePaidEvent
     */
    public void publishInvoicePaid(InvoiceEntity invoice) {
        InvoicePaidEvent event = new InvoicePaidEvent(invoice);
        publishEvent("invoice.paid", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, InvoiceEvent event) {
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
