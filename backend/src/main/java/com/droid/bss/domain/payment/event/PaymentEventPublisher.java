package com.droid.bss.domain.payment.event;

import com.droid.bss.domain.payment.PaymentEntity;
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
 * Publisher for Payment-related CloudEvents
 * Publishes events to Kafka topics following CloudEvents v1.0 specification
 */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish PaymentCreatedEvent
     */
    public void publishPaymentCreated(PaymentEntity payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(payment);
        publishEvent("payment.created", event);
    }

    /**
     * Publish PaymentProcessingEvent
     */
    public void publishPaymentProcessing(PaymentEntity payment) {
        PaymentProcessingEvent event = new PaymentProcessingEvent(payment);
        publishEvent("payment.processing", event);
    }

    /**
     * Publish PaymentCompletedEvent
     */
    public void publishPaymentCompleted(PaymentEntity payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(payment);
        publishEvent("payment.completed", event);
    }

    /**
     * Publish PaymentFailedEvent
     */
    public void publishPaymentFailed(PaymentEntity payment, String failureReason) {
        PaymentFailedEvent event = new PaymentFailedEvent(payment, failureReason);
        publishEvent("payment.failed", event);
    }

    /**
     * Publish PaymentRefundedEvent
     */
    public void publishPaymentRefunded(PaymentEntity payment, String refundReason) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(payment, refundReason);
        publishEvent("payment.refunded", event);
    }

    /**
     * Generic event publishing method using CloudEvents format
     */
    private void publishEvent(String topic, PaymentEvent event) {
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
