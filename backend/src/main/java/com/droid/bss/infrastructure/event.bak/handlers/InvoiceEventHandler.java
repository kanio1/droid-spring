package com.droid.bss.infrastructure.event.handlers;

import com.droid.bss.infrastructure.event.publisher.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Handles Invoice-related events.
 *
 * @since 1.0
 */
@Component
public class InvoiceEventHandler implements EventHandler<DomainEvent> {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEventHandler.class);

    @Override
    public boolean canHandle(DomainEvent event) {
        if (event == null) {
            return false;
        }
        String eventType = event.getType();
        return eventType != null && (
            eventType.startsWith("invoice.") ||
            eventType.startsWith("invoice.created") ||
            eventType.startsWith("invoice.updated") ||
            eventType.startsWith("invoice.paid") ||
            eventType.startsWith("invoice.overdue")
        );
    }

    @Override
    public EventHandlingResult handle(DomainEvent event) throws EventHandlingException {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Handling invoice event: id={}, type={}, subject={}",
                event.getId(), event.getType(), event.getSubject());

            // Handle specific event types
            String eventType = event.getType();
            if (eventType.contains("created")) {
                return handleInvoiceCreated(event, startTime);
            } else if (eventType.contains("updated")) {
                return handleInvoiceUpdated(event, startTime);
            } else if (eventType.contains("paid")) {
                return handleInvoicePaid(event, startTime);
            } else if (eventType.contains("overdue")) {
                return handleInvoiceOverdue(event, startTime);
            } else {
                return handleGenericInvoiceEvent(event, startTime);
            }

        } catch (Exception e) {
            log.error("Error handling invoice event: {}", event.getId(), e);
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
        return "invoice.*";
    }

    @Override
    public int getPriority() {
        return 8;
    }

    @Override
    public String getName() {
        return "InvoiceEventHandler";
    }

    // Private helper methods

    private EventHandlingResult handleInvoiceCreated(DomainEvent event, long startTime) {
        log.info("Invoice created: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleInvoiceUpdated(DomainEvent event, long startTime) {
        log.info("Invoice updated: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleInvoicePaid(DomainEvent event, long startTime) {
        log.info("Invoice paid: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleInvoiceOverdue(DomainEvent event, long startTime) {
        log.info("Invoice overdue: subject={}", event.getSubject());

        // Simulate processing
        simulateProcessing();

        return EventHandlingResult.success(
            event.getId(),
            event.getType(),
            getName(),
            System.currentTimeMillis() - startTime
        );
    }

    private EventHandlingResult handleGenericInvoiceEvent(DomainEvent event, long startTime) {
        log.info("Processing generic invoice event: type={}, subject={}",
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
