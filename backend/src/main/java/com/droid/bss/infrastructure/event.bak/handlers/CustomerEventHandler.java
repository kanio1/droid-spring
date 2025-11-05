package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles Customer-related events.
 *
 * @since 1.0
 */
@Component
public class CustomerEventHandler implements EventHandler<DomainEvent> {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventHandler.class);

    @Override
    public boolean canHandle(DomainEvent event) {
        if (event == null) {
            return false;
        }
        String eventType = event.getType();
        return eventType != null && (
            eventType.startsWith("customer.") ||
            eventType.startsWith("customer.created") ||
            eventType.startsWith("customer.updated") ||
            eventType.startsWith("customer.statusChanged") ||
            eventType.startsWith("customer.terminated")
        );
    }

    @Override
    public EventHandlingResult handle(DomainEvent event) throws EventHandlingException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Handling customer event: id={}, type={}, subject={}",
                event.getId(), event.getType(), event.getSubject());

            // Handle specific event types
            String eventType = event.getType();
            if (eventType.contains("created")) {
                return handleCustomerCreated(event, startTime);
            } else if (eventType.contains("updated")) {
                return handleCustomerUpdated(event, startTime);
            } else if (eventType.contains("statusChanged")) {
                return handleCustomerStatusChanged(event, startTime);
            } else if (eventType.contains("terminated")) {
                return handleCustomerTerminated(event, startTime);
            } else {
                return handleGenericCustomerEvent(event, startTime);
            }

        } catch (Exception e) {
            log.error("Error handling customer event: {}", event.getId(), e);
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
        return "customer.*";
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getName() {
        return "CustomerEventHandler";
    }

    // Private helper methods

    private EventHandlingResult handleCustomerCreated(DomainEvent event, long startTime) {
        log.info("Customer created: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleCustomerUpdated(DomainEvent event, long startTime) {
        log.info("Customer updated: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleCustomerStatusChanged(DomainEvent event, long startTime) {
        log.info("Customer status changed: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleCustomerTerminated(DomainEvent event, long startTime) {
        log.info("Customer terminated: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleGenericCustomerEvent(DomainEvent event, long startTime) {
        log.info("Processing generic customer event: type={}, subject={}",
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
