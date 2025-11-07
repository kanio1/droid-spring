package com.droid.bss.infrastructure.cache;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.order.event.OrderEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Event-Based Cache Invalidator
 *
 * Listens to domain events and triggers cache eviction to ensure data consistency
 * This is the critical missing link between the event system and cache invalidation
 */
@Component
public class EventBasedCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(EventBasedCacheInvalidator.class);
    private final CacheEvictionService cacheEvictionService;

    public EventBasedCacheInvalidator(CacheEvictionService cacheEvictionService) {
        this.cacheEvictionService = cacheEvictionService;
    }

    /**
     * Handle customer-related events
     * Triggered when customer data changes
     */
    @KafkaListener(topics = "customer.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-cache}")
    public void handleCustomerEvent(CustomerEvent event) {
        try {
            log.debug("Processing customer event for cache invalidation: {}", event.getType());

            String customerId = event.getCustomerId().toString();

            // Evict specific customer cache
            cacheEvictionService.evictCustomerCache(customerId);

            // Evict all customer list caches (since list might be affected)
            cacheEvictionService.evictAllCustomerListCaches();

            log.info("Cache invalidated for customer event: type={}, customerId={}",
                event.getType(), customerId);

        } catch (Exception e) {
            log.error("Failed to invalidate cache for customer event: {}", event.getType(), e);
        }
    }

    /**
     * Handle order-related events
     * Triggered when order data changes
     */
    @KafkaListener(topics = "order.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-cache}")
    public void handleOrderEvent(OrderEvent event) {
        try {
            log.debug("Processing order event for cache invalidation: {}", event.getType());

            UUID orderId = event.getOrderId();

            // Evict specific order cache
            cacheEvictionService.evictOrderCache(orderId);

            // Evict all order caches
            cacheEvictionService.evictAllOrderCaches();

            // If order is for a customer, also evict customer-related caches
            if (event.getCustomerId() != null) {
                cacheEvictionService.evictCustomerCache(event.getCustomerId().toString());
                cacheEvictionService.evictAllCustomerListCaches();
            }

            log.info("Cache invalidated for order event: type={}, orderId={}",
                event.getType(), orderId);

        } catch (Exception e) {
            log.error("Failed to invalidate cache for order event: {}", event.getType(), e);
        }
    }

    /**
     * Handle invoice-related events
     * Triggered when invoice data changes
     */
    @KafkaListener(topics = "invoice.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-cache}")
    public void handleInvoiceEvent(InvoiceEvent event) {
        try {
            log.debug("Processing invoice event for cache invalidation: {}", event.getType());

            UUID invoiceId = event.getInvoiceId();

            // Evict specific invoice cache
            cacheEvictionService.evictInvoiceCache(invoiceId);

            // Evict all invoice caches
            cacheEvictionService.evictAllInvoiceCaches();

            // If invoice is for a customer, also evict customer-related caches
            if (event.getCustomerId() != null) {
                cacheEvictionService.evictCustomerCache(event.getCustomerId().toString());
                cacheEvictionService.evictAllCustomerListCaches();
            }

            log.info("Cache invalidated for invoice event: type={}, invoiceId={}",
                event.getType(), invoiceId);

        } catch (Exception e) {
            log.error("Failed to invalidate cache for invoice event: {}", event.getType(), e);
        }
    }

    /**
     * Handle payment-related events
     * Triggered when payment data changes
     */
    @KafkaListener(topics = "payment.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-cache}")
    public void handlePaymentEvent(PaymentEvent event) {
        try {
            log.debug("Processing payment event for cache invalidation: {}", event.getType());

            UUID paymentId = event.getPaymentId();

            // Note: Payment cache eviction would be added here when payment queries are implemented

            // If payment is for a customer, also evict customer-related caches
            if (event.getCustomerId() != null) {
                cacheEvictionService.evictCustomerCache(event.getCustomerId().toString());
                cacheEvictionService.evictAllCustomerListCaches();
            }

            log.info("Cache invalidated for payment event: type={}, paymentId={}",
                event.getType(), paymentId);

        } catch (Exception e) {
            log.error("Failed to invalidate cache for payment event: {}", event.getType(), e);
        }
    }
}
