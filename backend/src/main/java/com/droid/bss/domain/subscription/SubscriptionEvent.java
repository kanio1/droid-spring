package com.droid.bss.domain.subscription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Subscription Domain Event
 * Used for broadcasting subscription-related events via RSocket and Kafka
 */
public class SubscriptionEvent {

    private final String eventType;
    private final UUID subscriptionId;
    private final UUID customerId;
    private final UUID productId;
    private final String productName;
    private final SubscriptionStatus status;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime timestamp;
    private final Map<String, Object> data;

    public SubscriptionEvent(String eventType, UUID subscriptionId, UUID customerId,
                           UUID productId, String productName, SubscriptionStatus status,
                           LocalDate startDate, LocalDate endDate,
                           LocalDateTime timestamp, Map<String, Object> data) {
        this.eventType = eventType;
        this.subscriptionId = subscriptionId;
        this.customerId = customerId;
        this.productId = productId;
        this.productName = productName;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static SubscriptionEvent created(UUID subscriptionId, UUID customerId,
                                           UUID productId, String productName,
                                           SubscriptionStatus status) {
        LocalDate today = LocalDate.now();
        return new SubscriptionEvent(
            "SUBSCRIPTION_CREATED",
            subscriptionId,
            customerId,
            productId,
            productName,
            status,
            today,
            today.plusYears(1),
            LocalDateTime.now(),
            Map.of("operation", "create")
        );
    }

    public static SubscriptionEvent cancelled(UUID subscriptionId, UUID customerId,
                                             UUID productId, String productName) {
        return new SubscriptionEvent(
            "SUBSCRIPTION_CANCELLED",
            subscriptionId,
            customerId,
            productId,
            productName,
            SubscriptionStatus.CANCELLED,
            null,
            LocalDate.now(),
            LocalDateTime.now(),
            Map.of("operation", "cancel", "cancelledAt", LocalDateTime.now())
        );
    }

    public static SubscriptionEvent renewed(UUID subscriptionId, UUID customerId,
                                           UUID productId, String productName) {
        LocalDate today = LocalDate.now();
        return new SubscriptionEvent(
            "SUBSCRIPTION_RENEWED",
            subscriptionId,
            customerId,
            productId,
            productName,
            SubscriptionStatus.ACTIVE,
            today,
            today.plusYears(1),
            LocalDateTime.now(),
            Map.of("operation", "renew", "renewedAt", LocalDateTime.now())
        );
    }

    public static SubscriptionEvent suspended(UUID subscriptionId, UUID customerId,
                                             UUID productId, String productName,
                                             String reason) {
        return new SubscriptionEvent(
            "SUBSCRIPTION_SUSPENDED",
            subscriptionId,
            customerId,
            productId,
            productName,
            SubscriptionStatus.SUSPENDED,
            null,
            null,
            LocalDateTime.now(),
            Map.of("operation", "suspend", "reason", reason)
        );
    }

    // Getters
    public String getEventType() { return eventType; }
    public UUID getSubscriptionId() { return subscriptionId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}
