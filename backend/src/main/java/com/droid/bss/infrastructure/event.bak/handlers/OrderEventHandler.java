package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles Order-related events.
 *
 * @since 1.0
 */
@Component
public class OrderEventHandler implements EventHandler<DomainEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderEventHandler.class);

    @Override
    public boolean canHandle(DomainEvent event) {
        if (event == null) {
            return false;
        }
        String eventType = event.getType();
        return eventType != null && (
            eventType.startsWith("order.") ||
            eventType.startsWith("order.created") ||
            eventType.startsWith("order.updated") ||
            eventType.startsWith("order.completed") ||
            eventType.startsWith("order.cancelled")
        );
    }

    @Override
    public EventHandlingResult handle(DomainEvent event) throws EventHandlingException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Handling order event: id={}, type={}, subject={}",
                event.getId(), event.getType(), event.getSubject());

            // Handle specific event types
            String eventType = event.getType();
            if (eventType.contains("created")) {
                return handleOrderCreated(event, startTime);
            } else if (eventType.contains("updated")) {
                return handleOrderUpdated(event, startTime);
            } else if (eventType.contains("completed")) {
                return handleOrderCompleted(event, startTime);
            } else if (eventType.contains("cancelled")) {
                return handleOrderCancelled(event, startTime);
            } else {
                return handleGenericOrderEvent(event, startTime);
            }

        } catch (Exception e) {
            log.error("Error handling order event: {}", event.getId(), e);
            return EventHandlingResult.failure(
                event.getId(),
                event.getType(),
                getName(),
                e.getMessage(),
                System.currentTimeMillis() - startTime
            );
        }
    }

    @Override
    public String getSupportedEventType() {
        return "order.*";
    }

    @Override
    public int getPriority() {
        return 9;
    }

    @Override
    public String getName() {
        return "OrderEventHandler";
    }

    // Private helper methods

    private EventHandlingResult handleOrderCreated(DomainEvent event, long startTime) {
        log.info("Order created: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleOrderUpdated(DomainEvent event, long startTime) {
        log.info("Order updated: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleOrderCompleted(DomainEvent event, long startTime) {
        log.info("Order completed: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleOrderCancelled(DomainEvent event, long startTime) {
        log.info("Order cancelled: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleGenericOrderEvent(DomainEvent event, long startTime) {
        log.info("Processing generic order event: type={}, subject={}",
            event.getType(), event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private void simulateProcessing() {
        try {
            // Simulate some processing time
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
