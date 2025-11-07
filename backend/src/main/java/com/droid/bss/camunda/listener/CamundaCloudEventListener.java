package com.droid.bss.camunda.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Camunda CloudEvent Listener
 *
 * Listens to CloudEvent format messages from Kafka and triggers Camunda workflows.
 * This handles the actual CloudEvent v1.0 format with JSON payload.
 */
@Component
public class CamundaCloudEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CamundaCloudEventListener.class);

    private final RuntimeService runtimeService;
    private final ObjectMapper objectMapper;

    public CamundaCloudEventListener(RuntimeService runtimeService, ObjectMapper objectMapper) {
        this.runtimeService = runtimeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle customer events in CloudEvent format
     */
    @KafkaListener(
            topics = {"customer.created", "customer.updated", "customer.statusChanged", "customer.terminated"},
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-camunda-workflow}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCustomerCloudEvent(
            @Payload CloudEvent cloudEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, CloudEvent> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = cloudEvent.getId();
        String eventType = cloudEvent.getType();

        logger.info("Received customer CloudEvent: {} from topic: {}", eventType, topic);

        try {
            // Parse event data
            JsonNode eventData = parseEventData(cloudEvent);

            switch (eventType) {
                case "com.droid.bss.customer.created.v1":
                    triggerCustomerOnboardingProcess(eventData, cloudEvent);
                    break;
                default:
                    logger.debug("No Camunda workflow for customer CloudEvent type: {}", eventType);
            }

            acknowledgment.acknowledge();
            logger.info("Successfully processed customer CloudEvent: {}", eventId);

        } catch (Exception e) {
            logger.error("Failed to process customer CloudEvent: {} - {}", eventType, eventId, e);
            // Don't rethrow to prevent infinite retries
        }
    }

    /**
     * Handle payment events in CloudEvent format
     */
    @KafkaListener(
            topics = {"payment.created", "payment.processing", "payment.completed", "payment.failed", "payment.refunded"},
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-camunda-workflow}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCloudEvent(
            @Payload CloudEvent cloudEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            ConsumerRecord<String, CloudEvent> record,
            Acknowledgment acknowledgment
    ) {
        String eventId = cloudEvent.getId();
        String eventType = cloudEvent.getType();

        logger.info("Received payment CloudEvent: {} from topic: {}", eventType, topic);

        try {
            // Parse event data
            JsonNode eventData = parseEventData(cloudEvent);

            switch (eventType) {
                case "com.droid.bss.payment.failed.v1":
                    triggerPaymentFailedRecoveryProcess(eventData, cloudEvent);
                    break;
                default:
                    logger.debug("No Camunda workflow for payment CloudEvent type: {}", eventType);
            }

            acknowledgment.acknowledge();
            logger.info("Successfully processed payment CloudEvent: {}", eventId);

        } catch (Exception e) {
            logger.error("Failed to process payment CloudEvent: {} - {}", eventType, eventId, e);
            // Don't rethrow to prevent infinite retries
        }
    }

    /**
     * Trigger Customer Onboarding workflow from CloudEvent data
     */
    private void triggerCustomerOnboardingProcess(JsonNode eventData, CloudEvent cloudEvent) {
        try {
            String customerId = eventData.get("customerId").asText();
            String firstName = getStringField(eventData, "firstName", "");
            String lastName = getStringField(eventData, "lastName", "");
            String email = getStringField(eventData, "email", "");

            logger.info("Triggering Customer Onboarding workflow for customer: {}", customerId);

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerId", customerId);
            variables.put("customerName", firstName + " " + lastName);
            variables.put("customerEmail", email);
            variables.put("customer_status", getStringField(eventData, "status", ""));
            variables.put("services", "basic,portal,support");

            // Start the Camunda process
            String processInstanceId = runtimeService.startProcessInstanceByKey(
                    "customerOnboarding",
                    customerId,
                    variables
            ).getId();

            logger.info("Started Customer Onboarding process instance: {} for customer: {}",
                    processInstanceId, customerId);

        } catch (Exception e) {
            logger.error("Failed to trigger Customer Onboarding workflow", e);
            throw e;
        }
    }

    /**
     * Trigger Payment Failed Recovery workflow from CloudEvent data
     */
    private void triggerPaymentFailedRecoveryProcess(JsonNode eventData, CloudEvent cloudEvent) {
        try {
            String paymentId = eventData.get("paymentId").asText();
            String customerId = eventData.get("customerId").asText();
            double amount = eventData.get("amount").asDouble();
            String currency = getStringField(eventData, "currency", "USD");
            String failureReason = getStringField(eventData, "failureReason", "Unknown");

            logger.info("Triggering Payment Failed Recovery workflow for payment: {}", paymentId);

            Map<String, Object> variables = new HashMap<>();
            variables.put("paymentId", paymentId);
            variables.put("customerId", customerId);
            variables.put("amount", amount);
            variables.put("currency", currency);
            variables.put("reason", failureReason);

            // Start the Camunda process
            String processInstanceId = runtimeService.startProcessInstanceByKey(
                    "paymentFailedRecovery",
                    paymentId,
                    variables
            ).getId();

            logger.info("Started Payment Failed Recovery process instance: {} for payment: {}",
                    processInstanceId, paymentId);

        } catch (Exception e) {
            logger.error("Failed to trigger Payment Failed Recovery workflow", e);
            throw e;
        }
    }

    /**
     * Parse CloudEvent data to JsonNode
     */
    private JsonNode parseEventData(CloudEvent cloudEvent) {
        try {
            if (cloudEvent.getData() != null) {
                String dataStr = new String(cloudEvent.getData().toBytes());
                return objectMapper.readTree(dataStr);
            }
            logger.warn("CloudEvent has no data: {}", cloudEvent.getId());
            return objectMapper.createObjectNode();
        } catch (Exception e) {
            logger.error("Failed to parse CloudEvent data: {}", cloudEvent.getId(), e);
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Helper method to get string field from JsonNode with default value
     */
    private String getStringField(JsonNode node, String fieldName, String defaultValue) {
        JsonNode field = node.get(fieldName);
        return field != null ? field.asText() : defaultValue;
    }
}
