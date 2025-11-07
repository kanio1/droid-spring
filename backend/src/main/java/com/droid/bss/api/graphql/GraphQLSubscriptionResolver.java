package com.droid.bss.api.graphql;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.domain.subscription.event.SubscriptionEvent;
import com.droid.bss.domain.order.event.OrderEvent;
import com.droid.bss.application.service.NotificationService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Subscription Resolver
 * Provides real-time event streaming for all domain events
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GraphQLSubscriptionResolver {

    private final NotificationService notificationService;
    private final Sinks.Many<Map<String, Object>> customerEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Map<String, Object>> invoiceEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Map<String, Object>> paymentEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Map<String, Object>> subscriptionEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Map<String, Object>> orderEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Map<String, Object>> systemEventSink = Sinks.many().multicast().onBackpressureBuffer();

    @SubscriptionMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<Map<String, Object>> customerEvents(DataFetchingEnvironment environment) {
        UUID customerId = environment.getArgument("customerId");

        log.info("Subscribing to customer events{}", customerId != null ? " for customer: " + customerId : "");

        return customerEventSink.asFlux()
            .filter(event -> {
                if (customerId == null) {
                    return true;
                }
                return customerId.equals(event.get("customerId"));
            })
            .map(this::formatCustomerEvent)
            .doOnNext(event -> log.debug("Sending customer event: {}", event.get("eventType")));
    }

    @SubscriptionMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<Map<String, Object>> invoiceEvents(DataFetchingEnvironment environment) {
        UUID customerId = environment.getArgument("customerId");

        log.info("Subscribing to invoice events{}", customerId != null ? " for customer: " + customerId : "");

        return invoiceEventSink.asFlux()
            .filter(event -> {
                if (customerId == null) {
                    return true;
                }
                return customerId.equals(event.get("customerId"));
            })
            .map(this::formatInvoiceEvent)
            .doOnNext(event -> log.debug("Sending invoice event: {}", event.get("eventType")));
    }

    @SubscriptionMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<Map<String, Object>> paymentEvents(DataFetchingEnvironment environment) {
        UUID customerId = environment.getArgument("customerId");

        log.info("Subscribing to payment events{}", customerId != null ? " for customer: " + customerId : "");

        return paymentEventSink.asFlux()
            .filter(event -> {
                if (customerId == null) {
                    return true;
                }
                return customerId.equals(event.get("customerId"));
            })
            .map(this::formatPaymentEvent)
            .doOnNext(event -> log.debug("Sending payment event: {}", event.get("eventType")));
    }

    @SubscriptionMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<Map<String, Object>> subscriptionEvents(DataFetchingEnvironment environment) {
        UUID customerId = environment.getArgument("customerId");

        log.info("Subscribing to subscription events{}", customerId != null ? " for customer: " + customerId : "");

        return subscriptionEventSink.asFlux()
            .filter(event -> {
                if (customerId == null) {
                    return true;
                }
                return customerId.equals(event.get("customerId"));
            })
            .map(this::formatSubscriptionEvent)
            .doOnNext(event -> log.debug("Sending subscription event: {}", event.get("eventType")));
    }

    @SubscriptionMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<Map<String, Object>> orderEvents(DataFetchingEnvironment environment) {
        UUID customerId = environment.getArgument("customerId");

        log.info("Subscribing to order events{}", customerId != null ? " for customer: " + customerId : "");

        return orderEventSink.asFlux()
            .filter(event -> {
                if (customerId == null) {
                    return true;
                }
                return customerId.equals(event.get("customerId"));
            })
            .map(this::formatOrderEvent)
            .doOnNext(event -> log.debug("Sending order event: {}", event.get("eventType")));
    }

    @SubscriptionMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Map<String, Object>> systemEvents() {
        log.info("Subscribing to system events");

        return systemEventSink.asFlux()
            .map(this::formatSystemEvent)
            .doOnNext(event -> log.debug("Sending system event: {}", event.get("eventType")));
    }

    // ========== EVENT BROADCASTING METHODS ==========

    public void broadcastCustomerEvent(CustomerEvent event) {
        String customerName = event.getFirstName() + " " + event.getLastName();
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getType(),
            "customerId", event.getCustomerId(),
            "customerName", customerName,
            "email", event.getEmail(),
            "status", event.getStatus().toString(),
            "timestamp", LocalDateTime.now()
        );

        customerEventSink.tryEmitNext(eventData);
    }

    public void broadcastInvoiceEvent(InvoiceEvent event) {
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getType(),
            "invoiceId", event.getInvoiceId(),
            "customerId", UUID.fromString(event.getCustomerId()),
            "orderId", null, // Not available in event
            "totalAmount", null, // Not available in event, would need entity lookup
            "status", event.getStatus().toString(),
            "timestamp", LocalDateTime.now()
        );

        invoiceEventSink.tryEmitNext(eventData);
    }

    public void broadcastPaymentEvent(PaymentEvent event) {
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getType(),
            "paymentId", event.getPaymentId(),
            "invoiceId", event.getInvoiceId() != null ? UUID.fromString(event.getInvoiceId()) : null,
            "customerId", UUID.fromString(event.getCustomerId()),
            "amount", event.getAmount(),
            "status", event.getStatus().toString(),
            "timestamp", LocalDateTime.now()
        );

        paymentEventSink.tryEmitNext(eventData);
    }

    public void broadcastSubscriptionEvent(SubscriptionEvent event) {
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getType(),
            "subscriptionId", event.getSubscriptionId(),
            "customerId", UUID.fromString(event.getCustomerId()),
            "productId", event.getProductId(),
            "productName", "Unknown", // Not available in event, would need entity lookup
            "status", event.getStatus().toString(),
            "timestamp", LocalDateTime.now()
        );

        subscriptionEventSink.tryEmitNext(eventData);
    }

    public void broadcastOrderEvent(OrderEvent event) {
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", event.getType(),
            "orderId", event.getOrderId(),
            "customerId", UUID.fromString(event.getCustomerId()),
            "status", event.getStatus().toString(),
            "timestamp", LocalDateTime.now()
        );

        orderEventSink.tryEmitNext(eventData);
    }

    public void broadcastSystemEvent(String eventType, String severity, String message) {
        Map<String, Object> eventData = Map.of(
            "eventId", UUID.randomUUID().toString(),
            "eventType", eventType,
            "severity", severity,
            "message", message,
            "timestamp", LocalDateTime.now()
        );

        systemEventSink.tryEmitNext(eventData);
    }

    // ========== EVENT FORMATTING ==========

    private Map<String, Object> formatCustomerEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "customer", Map.of(
                "id", rawEvent.get("customerId"),
                "name", rawEvent.get("customerName"),
                "email", rawEvent.get("email"),
                "status", rawEvent.get("status")
            ),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }

    private Map<String, Object> formatInvoiceEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "invoice", Map.of(
                "id", rawEvent.get("invoiceId"),
                "customerId", rawEvent.get("customerId"),
                "orderId", rawEvent.get("orderId"),
                "totalAmount", rawEvent.get("totalAmount"),
                "status", rawEvent.get("status")
            ),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }

    private Map<String, Object> formatPaymentEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "payment", Map.of(
                "id", rawEvent.get("paymentId"),
                "invoiceId", rawEvent.get("invoiceId"),
                "customerId", rawEvent.get("customerId"),
                "amount", rawEvent.get("amount"),
                "status", rawEvent.get("status")
            ),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }

    private Map<String, Object> formatSubscriptionEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "subscription", Map.of(
                "id", rawEvent.get("subscriptionId"),
                "customerId", rawEvent.get("customerId"),
                "productId", rawEvent.get("productId"),
                "productName", rawEvent.get("productName"),
                "status", rawEvent.get("status")
            ),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }

    private Map<String, Object> formatOrderEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "order", Map.of(
                "id", rawEvent.get("orderId"),
                "customerId", rawEvent.get("customerId"),
                "status", rawEvent.get("status")
            ),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }

    private Map<String, Object> formatSystemEvent(Map<String, Object> rawEvent) {
        return Map.of(
            "eventId", rawEvent.get("eventId"),
            "eventType", rawEvent.get("eventType"),
            "severity", rawEvent.get("severity"),
            "message", rawEvent.get("message"),
            "timestamp", rawEvent.get("timestamp"),
            "data", rawEvent
        );
    }
}
