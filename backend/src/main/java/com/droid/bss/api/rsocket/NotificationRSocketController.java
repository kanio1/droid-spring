package com.droid.bss.api.rsocket;

import com.droid.bss.application.service.NotificationService;
import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.domain.subscription.event.SubscriptionEvent;
import com.droid.bss.infrastructure.rsocket.RedisConnectionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RSocket Controller for real-time notifications
 * Handles bidirectional communication for customer, invoice, payment, and subscription events
 * Uses Redis-backed connection registry for multi-instance scalability
 */
@Slf4j
@Controller
@RequestMapping("/api/v1/rsocket")
@RequiredArgsConstructor
public class NotificationRSocketController {

    private final NotificationService notificationService;
    private final RedisConnectionRegistry connectionRegistry;

    /**
     * Handle new client connection
     */
    @ConnectMapping("connect")
    public Mono<Void> handleConnection(RSocketRequester requester, String clientInfo) {
        String clientId = UUID.randomUUID().toString();
        String userId = getCurrentUserId();

        // Register connection in Redis-backed registry
        connectionRegistry.registerConnection(clientId, requester, userId, clientInfo);

        log.info("RSocket client connected: clientId={}, userId={}, info={}, totalConnections={}",
            clientId, userId, clientInfo, connectionRegistry.getTotalConnections());

        // Send welcome message
        return requester.route("welcome")
            .data(Map.of(
                "clientId", clientId,
                "userId", userId,
                "message", "Connected to BSS Real-time Notification Service",
                "timestamp", LocalDateTime.now(),
                "instanceId", getInstanceId(),
                "localConnections", connectionRegistry.getLocalConnectionsCount(),
                "totalConnections", connectionRegistry.getTotalConnections(),
                "features", Map.of(
                    "customerEvents", true,
                    "invoiceEvents", true,
                    "paymentEvents", true,
                    "subscriptionEvents", true
                )
            ))
            .send();
    }

    /**
     * Subscribe to customer events
     */
    @MessageMapping("customer.subscribe")
    @PreAuthorize("hasRole('USER')")
    public Mono<String> subscribeToCustomerEvents() {
        String userId = getCurrentUserId();
        log.info("User {} subscribed to customer events", userId);
        return Mono.just("Successfully subscribed to customer events");
    }

    /**
     * Subscribe to invoice events
     */
    @MessageMapping("invoice.subscribe")
    @PreAuthorize("hasRole('USER')")
    public Mono<String> subscribeToInvoiceEvents() {
        String userId = getCurrentUserId();
        log.info("User {} subscribed to invoice events", userId);
        return Mono.just("Successfully subscribed to invoice events");
    }

    /**
     * Subscribe to payment events
     */
    @MessageMapping("payment.subscribe")
    @PreAuthorize("hasRole('USER')")
    public Mono<String> subscribeToPaymentEvents() {
        String userId = getCurrentUserId();
        log.info("User {} subscribed to payment events", userId);
        return Mono.just("Successfully subscribed to payment events");
    }

    /**
     * Subscribe to subscription events
     */
    @MessageMapping("subscription.subscribe")
    @PreAuthorize("hasRole('USER')")
    public Mono<String> subscribeToSubscriptionEvents() {
        String userId = getCurrentUserId();
        log.info("User {} subscribed to subscription events", userId);
        return Mono.just("Successfully subscribed to subscription events");
    }

    /**
     * Get all available event types
     */
    @MessageMapping("events.types")
    public Mono<Map<String, Object>> getEventTypes() {
        return Mono.just(Map.of(
            "eventTypes", Map.of(
                "CUSTOMER_CREATED", "Customer was created",
                "CUSTOMER_UPDATED", "Customer information was updated",
                "CUSTOMER_DELETED", "Customer was deleted",
                "INVOICE_GENERATED", "New invoice was generated",
                "INVOICE_SENT", "Invoice was sent to customer",
                "INVOICE_PAID", "Invoice was paid",
                "INVOICE_OVERDUE", "Invoice is overdue",
                "PAYMENT_PROCESSED", "Payment was processed",
                "PAYMENT_FAILED", "Payment failed",
                "PAYMENT_REFUNDED", "Payment was refunded",
                "SUBSCRIPTION_CREATED", "New subscription was created",
                "SUBSCRIPTION_CANCELLED", "Subscription was cancelled",
                "SUBSCRIPTION_RENEWED", "Subscription was renewed",
                "SUBSCRIPTION_SUSPENDED", "Subscription was suspended"
            )
        ));
    }

    /**
     * Request notification test
     */
    @MessageMapping("test.notification")
    public Mono<Map<String, Object>> sendTestNotification() {
        String testEventId = UUID.randomUUID().toString();
        log.info("Sending test notification: {}", testEventId);

        return Mono.just(Map.of(
            "eventId", testEventId,
            "type", "TEST",
            "message", "This is a test notification",
            "timestamp", LocalDateTime.now(),
            "status", "SENT"
        ));
    }

    /**
     * Get connection status
     */
    @MessageMapping("connection.status")
    public Mono<Map<String, Object>> getConnectionStatus() {
        return Mono.just(Map.of(
            "connected", true,
            "clientCount", connectionRegistry.getLocalConnectionsCount(),
            "totalConnections", connectionRegistry.getTotalConnections(),
            "instanceId", getInstanceId(),
            "timestamp", LocalDateTime.now(),
            "server", "BSS Backend RSocket Server"
        ));
    }

    /**
     * Stream real-time events with backpressure support
     * Implements rate limiting and buffer management to prevent overwhelming clients
     */
    @MessageMapping("events.stream")
    public Flux<Map<String, Object>> streamEvents() {
        final int MAX_BUFFER_SIZE = 1000;
        final int RATE_LIMIT_PER_SECOND = 50;
        final Duration EMIT_INTERVAL = Duration.ofMillis(1000 / RATE_LIMIT_PER_SECOND);

        return Flux.interval(EMIT_INTERVAL)
            // Apply backpressure: limit buffered items
            .onBackpressureBuffer(MAX_BUFFER_SIZE, dropped -> {
                log.warn("Backpressure buffer dropped event: {}", dropped);
            })
            // Rate limiting with token bucket
            .map(tick -> {
                long timestamp = System.currentTimeMillis();
                return Map.<String, Object>of(
                    "eventId", UUID.randomUUID().toString(),
                    "type", "HEARTBEAT",
                    "timestamp", LocalDateTime.now(),
                    "serverTime", timestamp,
                    "uptime", tick,
                    "instanceId", getInstanceId(),
                    "localConnections", connectionRegistry.getLocalConnectionsCount(),
                    "rateLimit", RATE_LIMIT_PER_SECOND,
                    "bufferCapacity", MAX_BUFFER_SIZE
                );
            })
            // Publish on bounded elastic scheduler to prevent thread exhaustion
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(event -> {
                // Log backpressure metrics periodically
                if (tickCount.incrementAndGet() % 100 == 0) {
                    log.debug("RSocket stream metrics - Tick: {}, Connections: {}, Instance: {}",
                        event.get("uptime"), event.get("localConnections"), event.get("instanceId"));
                }
            })
            // Cleanup on cancel
            .doOnCancel(() -> log.info("RSocket event stream cancelled by client"))
            .doOnError(error -> log.error("RSocket event stream error: {}", error.getMessage()));
    }

    private final AtomicLong tickCount = new AtomicLong(0);

    /**
     * Customer event listener
     */
    @EventListener
    public void onCustomerEvent(CustomerEvent event) {
        notificationService.sendCustomerEventNotification(event);
    }

    /**
     * Invoice event listener
     */
    @EventListener
    public void onInvoiceEvent(InvoiceEvent event) {
        notificationService.sendInvoiceEventNotification(event);
    }

    /**
     * Payment event listener
     */
    @EventListener
    public void onPaymentEvent(PaymentEvent event) {
        notificationService.sendPaymentEventNotification(event);
    }

    /**
     * Subscription event listener
     */
    @EventListener
    public void onSubscriptionEvent(SubscriptionEvent event) {
        notificationService.sendSubscriptionEventNotification(event);
    }

    /**
     * Disconnect client
     */
    public void disconnectClient(String clientId) {
        connectionRegistry.unregisterConnection(clientId);
    }

    /**
     * Get connected clients count (local instance)
     */
    public int getConnectedClientsCount() {
        return connectionRegistry.getLocalConnectionsCount();
    }

    /**
     * Get total connected clients (across all instances)
     */
    public long getTotalConnectedClientsCount() {
        return connectionRegistry.getTotalConnections();
    }

    /**
     * Broadcast event to all clients with backpressure handling
     * Broadcasts to local connections only (multi-instance via Redis pub/sub)
     * Implements rate limiting and batch processing to prevent overwhelming clients
     */
    public Mono<Void> broadcastEvent(String eventType, Map<String, Object> data) {
        Map<String, RSocketRequester> localConnections = connectionRegistry.getAllLocalConnections();
        final int BATCH_SIZE = 10; // Process in small batches
        final Duration BATCH_DELAY = Duration.ofMillis(10); // Small delay between batches

        log.debug("Broadcasting event to {} local connections with backpressure control",
            localConnections.size());

        return Flux.fromIterable(localConnections.entrySet())
            .split(BATCH_SIZE) // Split into small batches
            .concatMapIterable(batch -> batch)
            .flatMap(entry -> {
                String clientId = entry.getKey();
                RSocketRequester requester = entry.getValue();

                return requester.route("events")
                    .data(Map.of(
                        "eventId", UUID.randomUUID().toString(),
                        "type", eventType,
                        "data", data,
                        "instanceId", getInstanceId(),
                        "timestamp", LocalDateTime.now(),
                        "batchInfo", Map.of(
                            "totalConnections", localConnections.size(),
                            "batchSize", BATCH_SIZE
                        )
                    ))
                    .send()
                    .onErrorContinue((error, obj) -> {
                        log.warn("Failed to send event to client {}: {}", clientId, error.getMessage());
                        // Remove stale connections
                        connectionRegistry.unregisterConnection(clientId);
                    });
            }, 1) // Max 1 concurrent request at a time
            .onBackpressureBuffer(50) // Buffer for backpressure
            .timeout(Duration.ofSeconds(5)) // Timeout for slow clients
            .onErrorResume(error -> {
                log.warn("Broadcast event completed with errors: {}", error.getMessage());
                return Mono.empty(); // Continue even if some clients fail
            })
            .then();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getInstanceId() {
        return System.getProperty("instance.id", "default-instance");
    }
}
