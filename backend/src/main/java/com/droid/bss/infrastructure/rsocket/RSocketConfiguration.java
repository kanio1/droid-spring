package com.droid.bss.infrastructure.rsocket;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.domain.subscription.event.SubscriptionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.rsocket.RSocketConnector;
import org.springframework.rsocket.core.DefaultRSocketConnector;
import org.springframework.rsocket.core.RSocketConnectorConfigurer;
import org.springframework.rsocket.transport.NettyTcpClientTransport;
import org.springframework.rsocket.transport.TcpClientTransport;
import io.rsocket.core.Resume;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSocket Configuration
 * Configures RSocket for real-time bidirectional communication
 * Uses Redis-backed connection registry for multi-instance scalability
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class RSocketConfiguration {

    private final RedisConnectionRegistry connectionRegistry;

    /**
     * Configure RSocket connector with backpressure support
     */
    public RSocketConnector rsocketConnector() {
        DefaultRSocketConnector connector = new DefaultRSocketConnector();

        // Enable auto-connect
        connector.setAutoConnect(true);

        // Configure backpressure and flow control
        // Keep-alive to detect dead connections
        connector.setKeepAlive(Duration.ofSeconds(20), Duration.ofSeconds(15));

        // Resume support for connection recovery
        Resume resume = new Resume()
            .sessionDuration(Duration.ofMinutes(5)) // Resume session duration
            .streamTimeout(Duration.ofSeconds(30)); // Stream timeout

        connector.setResume(resume);

        // Setup complete
        log.info("RSocket connector configured with backpressure support: keepAlive=20s, session=5min");

        return connector;
    }

    /**
     * Create TCP client transport
     */
    public TcpClientTransport tcpClientTransport() {
        return NettyTcpClientTransport.create("localhost", 7000);
    }

    /**
     * Handle new client connections
     */
    @ConnectMapping("client-info")
    public Mono<Void> handleConnect(RSocketRequester requester, String clientInfo) {
        String clientId = UUID.randomUUID().toString();
        String userId = getCurrentUserId();

        // Register connection in Redis-backed registry
        connectionRegistry.registerConnection(clientId, requester, userId, clientInfo);

        log.info("New RSocket client connected: {} - {}", clientId, clientInfo);

        // Send welcome message
        return requester.route("welcome")
            .data(Map.of(
                "clientId", clientId,
                "userId", userId,
                "message", "Connected to BSS Backend",
                "timestamp", LocalDateTime.now().toString(),
                "instanceId", getInstanceId(),
                "totalConnections", connectionRegistry.getTotalConnections()
            ))
            .send();
    }

    /**
     * Send notification to all connected clients with backpressure handling
     * Broadcasts to local connections only (multi-instance via Redis pub/sub)
     * Implements batch processing and rate limiting to prevent overwhelming clients
     */
    public Mono<Void> broadcastNotification(String type, Object data) {
        Map<String, RSocketRequester> localConnections = connectionRegistry.getAllLocalConnections();
        final int BATCH_SIZE = 10; // Process connections in batches
        final Duration SEND_TIMEOUT = Duration.ofSeconds(5); // Timeout for slow clients
        final int BACKPRESSURE_BUFFER_SIZE = 100; // Buffer for backpressure

        log.debug("Broadcasting notification to {} local connections with backpressure",
            localConnections.size());

        return Flux.fromIterable(localConnections.entrySet())
            .buffer(BATCH_SIZE) // Buffer connections in batches
            .concatMap(batch -> {
                // Process each batch
                return Flux.fromIterable(batch)
                    .parallel(Runtime.getRuntime().availableProcessors())
                    .runOn(Schedulers.parallel())
                    .flatMap(entry -> {
                        String clientId = entry.getKey();
                        RSocketRequester requester = entry.getValue();
                        return requester.route("notification")
                            .data(Map.of(
                                "id", UUID.randomUUID().toString(),
                                "type", type,
                                "data", data,
                                "instanceId", getInstanceId(),
                                "timestamp", LocalDateTime.now().toString()
                            ))
                            .timeout(SEND_TIMEOUT)
                            .onErrorContinue((error, obj) -> {
                                log.warn("Failed to send notification to client {}: {}",
                                    clientId, error.getMessage());
                                // Remove stale connections
                                connectionRegistry.unregisterConnection(clientId);
                            })
                            .onBackpressureBuffer(BACKPRESSURE_BUFFER_SIZE);
                    }, 1) // Max 1 concurrent per batch
                    .sequential()
                    .then();
            }, 1) // Process one batch at a time
            .onErrorResume(error -> {
                log.warn("Broadcast notification error: {}", error.getMessage());
                return Mono.empty(); // Continue even if some notifications fail
            })
            .then();
    }

    /**
     * Send customer event notification with backpressure handling
     */
    public Mono<Void> sendCustomerEvent(CustomerEvent event) {
        return broadcastNotification("CUSTOMER_EVENT", Map.of(
            "eventType", event.getType(),
            "customerId", event.getCustomerId(),
            "customerName", event.getFirstName() + " " + event.getLastName(),
            "email", event.getEmail(),
            "status", event.getStatus(),
            "timestamp", event.getTime()
        ));
    }

    /**
     * Send invoice event notification with backpressure handling
     */
    public Mono<Void> sendInvoiceEvent(InvoiceEvent event) {
        return broadcastNotification("INVOICE_EVENT", Map.of(
            "eventType", event.getType(),
            "invoiceId", event.getInvoiceId(),
            "customerId", event.getCustomerId(),
            "invoiceNumber", event.getInvoiceNumber(),
            "status", event.getStatus(),
            "timestamp", event.getTime()
        ));
    }

    /**
     * Send payment event notification with backpressure handling
     */
    public Mono<Void> sendPaymentEvent(PaymentEvent event) {
        return broadcastNotification("PAYMENT_EVENT", Map.of(
            "eventType", event.getType(),
            "paymentId", event.getPaymentId(),
            "paymentNumber", event.getPaymentNumber(),
            "customerId", event.getCustomerId(),
            "invoiceId", event.getInvoiceId(),
            "amount", event.getAmount(),
            "currency", event.getCurrency(),
            "status", event.getStatus(),
            "timestamp", event.getTime()
        ));
    }

    /**
     * Send subscription event notification with backpressure handling
     */
    public Mono<Void> sendSubscriptionEvent(SubscriptionEvent event) {
        return broadcastNotification("SUBSCRIPTION_EVENT", Map.of(
            "eventType", event.getType(),
            "subscriptionId", event.getSubscriptionId(),
            "subscriptionNumber", event.getSubscriptionNumber(),
            "customerId", event.getCustomerId(),
            "productId", event.getProductId(),
            "status", event.getStatus(),
            "price", event.getPrice(),
            "currency", event.getCurrency(),
            "timestamp", event.getTime()
        ));
    }

    /**
     * Heartbeat to keep connections alive
     */
    @MessageMapping("ping")
    public Mono<String> ping() {
        return Mono.just("pong - " + System.currentTimeMillis());
    }

    /**
     * Get connected clients count (local instance only)
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
     * Clean up disconnected clients
     */
    public void removeClient(String clientId) {
        connectionRegistry.unregisterConnection(clientId);
    }

    /**
     * Update connection last seen timestamp
     */
    public void updateClientLastSeen(String clientId) {
        connectionRegistry.updateLastSeen(clientId);
    }

    /**
     * Scheduled cleanup of stale connections
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStaleConnections() {
        log.debug("Running scheduled cleanup of stale RSocket connections");
        connectionRegistry.cleanupStaleConnections();
    }

    /**
     * Get authenticated user ID from security context
     */
    protected String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    /**
     * Get current instance ID
     */
    private String getInstanceId() {
        return System.getProperty("instance.id", "default-instance");
    }
}
