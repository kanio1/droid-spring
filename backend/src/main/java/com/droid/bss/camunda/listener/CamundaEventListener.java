package com.droid.bss.camunda.listener;

import com.droid.bss.domain.customer.event.CustomerCreatedEvent;
import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.domain.payment.event.PaymentFailedEvent;
import org.camunda.bpm.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Camunda Event Listener
 *
 * Listens to domain events and triggers Camunda workflow processes.
 * This bridges the Kafka events with Camunda BPM processes.
 */
@Component
public class CamundaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CamundaEventListener.class);

    private final RuntimeService runtimeService;

    public CamundaEventListener(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /**
     * Handle customer created event
     */
    @KafkaListener(
            topics = "customer.created",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-camunda-workflow}"
    )
    public void handleCustomerCreatedEvent(CustomerCreatedEvent event) {
        logger.info("Received customer created event for Camunda workflow: {}", event.getCustomerId());

        try {
            triggerCustomerOnboardingProcess(event);
        } catch (Exception e) {
            logger.error("Failed to trigger Camunda workflow from customer event", e);
            throw e;
        }
    }

    /**
     * Handle payment failed event
     */
    @KafkaListener(
            topics = "payment.failed",
            groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-camunda-workflow}"
    )
    public void handlePaymentFailedEvent(PaymentFailedEvent event) {
        logger.info("Received payment failed event for Camunda workflow: {}", event.getPaymentId());

        try {
            triggerPaymentFailedRecoveryProcess(event);
        } catch (Exception e) {
            logger.error("Failed to trigger Camunda workflow from payment event", e);
            throw e;
        }
    }

    /**
     * Trigger Customer Onboarding workflow
     */
    private void triggerCustomerOnboardingProcess(CustomerCreatedEvent event) {
        logger.info("Triggering Customer Onboarding workflow for customer: {}", event.getCustomerId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("customerId", event.getCustomerId().toString());
        variables.put("customerName", event.getFirstName() + " " + event.getLastName());
        variables.put("customerEmail", event.getEmail());
        variables.put("customer_status", event.getStatus().toString());
        variables.put("services", "basic,portal,support");

        // Start the Camunda process
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                "customerOnboarding",
                event.getCustomerId().toString(),
                variables
        ).getId();

        logger.info("Started Customer Onboarding process instance: {} for customer: {}",
                processInstanceId, event.getCustomerId());
    }

    /**
     * Trigger Payment Failed Recovery workflow
     */
    private void triggerPaymentFailedRecoveryProcess(PaymentFailedEvent event) {
        logger.info("Triggering Payment Failed Recovery workflow for payment: {}", event.getPaymentId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("paymentId", event.getPaymentId().toString());
        variables.put("customerId", event.getCustomerId());
        variables.put("amount", event.getAmount().doubleValue());
        variables.put("currency", event.getCurrency());
        variables.put("reason", event.getFailureReason());

        // Start the Camunda process
        String processInstanceId = runtimeService.startProcessInstanceByKey(
                "paymentFailedRecovery",
                event.getPaymentId().toString(),
                variables
        ).getId();

        logger.info("Started Payment Failed Recovery process instance: {} for payment: {}",
                processInstanceId, event.getPaymentId());
    }
}
