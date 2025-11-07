package com.droid.bss.infrastructure.messaging.events;

import com.droid.bss.api.events.EventsController;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Event Stream Service
 *
 * Bridges Kafka events with Server-Sent Events (SSE)
 */
@Service
public class EventStreamService {

    private static final Logger log = LoggerFactory.getLogger(EventStreamService.class);

    private final EventsController eventsController;

    public EventStreamService(EventsController eventsController) {
        this.eventsController = eventsController;
    }

    /**
     * Listen to customer events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.customer", groupId = "bss-sse-bridge")
    public void handleCustomerEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header("kafka_receivedPartitionId") int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting customer event: {} from topic: {}", eventType, topic);

            // Broadcast to SSE clients
            eventsController.broadcastEvent("customer." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling customer event", e);
            acknowledgment.acknowledge(); // Acknowledge anyway to avoid reprocessing
        }
    }

    /**
     * Listen to order events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.order", groupId = "bss-sse-bridge")
    public void handleOrderEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting order event: {} from topic: {}", eventType, topic);

            eventsController.broadcastEvent("order." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling order event", e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to payment events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.payment", groupId = "bss-sse-bridge")
    public void handlePaymentEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting payment event: {} from topic: {}", eventType, topic);

            eventsController.broadcastEvent("payment." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling payment event", e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to invoice events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.invoice", groupId = "bss-sse-bridge")
    public void handleInvoiceEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting invoice event: {} from topic: {}", eventType, topic);

            eventsController.broadcastEvent("invoice." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling invoice event", e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to subscription events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.subscription", groupId = "bss-sse-bridge")
    public void handleSubscriptionEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting subscription event: {} from topic: {}", eventType, topic);

            eventsController.broadcastEvent("subscription." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling subscription event", e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Listen to system events and broadcast via SSE
     */
    @KafkaListener(topics = "bss.events.system", groupId = "bss-sse-bridge")
    public void handleSystemEvent(
            @Payload CloudEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {
        try {
            String eventType = event.getType();
            log.debug("Broadcasting system event: {} from topic: {}", eventType, topic);

            eventsController.broadcastEvent("system." + eventType, event);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error handling system event", e);
            acknowledgment.acknowledge();
        }
    }
}
