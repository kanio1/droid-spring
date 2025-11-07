package com.droid.bss.application.service;

import com.droid.bss.domain.customer.CustomerEvent;
import com.droid.bss.domain.invoice.InvoiceEvent;
import com.droid.bss.domain.payment.PaymentEvent;
import com.droid.bss.domain.subscription.SubscriptionEvent;
import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing real-time notifications via RSocket
 * Uses Redis-backed connection registry for multi-instance scalability
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RedisConnectionRegistry connectionRegistry;

    /**
     * Send customer event notification
     */
    public Mono<Void> sendCustomerEventNotification(CustomerEvent event) {
        return broadcast("customer.event", Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getEventType(),
            "customerId", event.getCustomerId(),
            "customerName", event.getCustomerName(),
            "email", event.getEmail(),
            "status", event.getStatus(),
            "timestamp", event.getTimestamp(),
            "data", event.getData()
        ));
    }

    /**
     * Send invoice event notification
     */
    public Mono<Void> sendInvoiceEventNotification(InvoiceEvent event) {
        return broadcast("invoice.event", Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getEventType(),
            "invoiceId", event.getInvoiceId(),
            "customerId", event.getCustomerId(),
            "orderId", event.getOrderId(),
            "totalAmount", event.getTotalAmount(),
            "status", event.getStatus(),
            "dueDate", event.getDueDate(),
            "timestamp", event.getTimestamp(),
            "data", event.getData()
        ));
    }

    /**
     * Send payment event notification
     */
    public Mono<Void> sendPaymentEventNotification(PaymentEvent event) {
        return broadcast("payment.event", Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getEventType(),
            "paymentId", event.getPaymentId(),
            "invoiceId", event.getInvoiceId(),
            "customerId", event.getCustomerId(),
            "amount", event.getAmount(),
            "status", event.getStatus(),
            "paymentMethod", event.getPaymentMethod(),
            "timestamp", event.getTimestamp(),
            "data", event.getData()
        ));
    }

    /**
     * Send subscription event notification
     */
    public Mono<Void> sendSubscriptionEventNotification(SubscriptionEvent event) {
        return broadcast("subscription.event", Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getEventType(),
            "subscriptionId", event.getSubscriptionId(),
            "customerId", event.getCustomerId(),
            "productId", event.getProductId(),
            "productName", event.getProductName(),
            "status", event.getStatus(),
            "startDate", event.getStartDate(),
            "endDate", event.getEndDate(),
            "timestamp", event.getTimestamp(),
            "data", event.getData()
        ));
    }

    /**
     * Broadcast event to all subscribers
     * Broadcasts to local connections only (multi-instance via Redis pub/sub)
     */
    private Mono<Void> broadcast(String eventType, Map<String, Object> data) {
        Map<String, RSocketRequester> localConnections = connectionRegistry.getAllLocalConnections();

        if (localConnections.isEmpty()) {
            log.debug("No local connections to broadcast event: {}", eventType);
            return Mono.empty();
        }

        log.debug("Broadcasting event '{}' to {} local connections", eventType, localConnections.size());

        return Flux.fromIterable(localConnections.values())
            .flatMap(requester -> requester.route("events")
                .data(Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "type", eventType,
                    "data", data,
                    "instanceId", getInstanceId(),
                    "timestamp", LocalDateTime.now()
                ))
                .send()
                .onErrorContinue((error, obj) -> {
                    log.warn("Failed to send event to subscriber: {}", error.getMessage());
                }))
            .then();
    }

    /**
     * Get number of active subscribers (local instance)
     */
    public int getSubscriberCount() {
        return connectionRegistry.getLocalConnectionsCount();
    }

    /**
     * Get total number of active subscribers (across all instances)
     */
    public long getTotalSubscriberCount() {
        return connectionRegistry.getTotalConnections();
    }

    private String getInstanceId() {
        return System.getProperty("instance.id", "default-instance");
    }
}
