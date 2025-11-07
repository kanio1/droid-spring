package com.droid.bss.infrastructure.cache.integration;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.infrastructure.cache.SmartCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Smart Cache Event Handler
 * Integrates SmartCacheService with domain events for intelligent cache management
 */
@Component
public class SmartCacheEventHandler {

    private static final Logger log = LoggerFactory.getLogger(SmartCacheEventHandler.class);
    private final SmartCacheService smartCacheService;

    public SmartCacheEventHandler(SmartCacheService smartCacheService) {
        this.smartCacheService = smartCacheService;
    }

    /**
     * Handle customer events with smart cache operations
     */
    @KafkaListener(topics = "customer.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-smart-cache}")
    public void handleCustomerEvent(CustomerEvent event) {
        try {
            log.debug("Processing customer event with SmartCache: {}", event.getType());

            String customerId = event.getCustomerId().toString();

            switch (event.getType()) {
                case CREATED:
                    // Pre-warm cache for new customer (especially if VIP)
                    smartCacheService.prewarmCustomerCache(customerId);
                    log.info("Pre-warmed cache for new customer: {}", customerId);
                    break;

                case UPDATED:
                    // Smart invalidation with re-warming for VIP customers
                    smartCacheService.onCustomerUpdated(customerId);
                    log.info("Smart cache invalidation for customer update: {}", customerId);
                    break;

                case STATUS_CHANGED:
                    // Invalidate all related caches
                    smartCacheService.onCustomerUpdated(customerId);
                    log.info("Cache invalidated for customer status change: {}", customerId);
                    break;

                default:
                    log.debug("Unhandled customer event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Failed to process customer event in SmartCache: {}", event.getType(), e);
        }
    }

    /**
     * Handle invoice events with smart cache operations
     */
    @KafkaListener(topics = "invoice.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-smart-cache}")
    public void handleInvoiceEvent(InvoiceEvent event) {
        try {
            log.debug("Processing invoice event with SmartCache: {}", event.getType());

            String customerId = event.getCustomerId() != null ? event.getCustomerId().toString() : null;
            String invoiceId = event.getInvoiceId().toString();

            switch (event.getType()) {
                case CREATED, UPDATED, STATUS_CHANGED, PAID:
                    if (customerId != null) {
                        smartCacheService.onInvoiceUpdated(customerId, invoiceId);
                        log.info("Smart cache invalidation for invoice event: {} for customer: {}",
                            event.getType(), customerId);
                    }
                    break;

                default:
                    log.debug("Unhandled invoice event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Failed to process invoice event in SmartCache: {}", event.getType(), e);
        }
    }

    /**
     * Handle payment events with smart cache operations
     */
    @KafkaListener(topics = "payment.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-smart-cache}")
    public void handlePaymentEvent(PaymentEvent event) {
        try {
            log.debug("Processing payment event with SmartCache: {}", event.getType());

            String customerId = event.getCustomerId() != null ? event.getCustomerId().toString() : null;

            switch (event.getType()) {
                case COMPLETED, FAILED, REFUNDED:
                    if (customerId != null) {
                        smartCacheService.onPaymentUpdated(customerId);
                        log.info("Smart cache invalidation for payment event: {} for customer: {}",
                            event.getType(), customerId);
                    }
                    break;

                default:
                    log.debug("Unhandled payment event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Failed to process payment event in SmartCache: {}", event.getType(), e);
        }
    }
}
