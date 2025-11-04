package com.droid.bss.domain.subscription.event;

import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Subscription-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class SubscriptionEvent {

    /**
     * CloudEvents required fields
     */
    protected final String id;
    protected final String source;
    protected final String type;
    protected final String specversion;
    protected final String datacontenttype;
    protected final LocalDateTime time;

    /**
     * Event data
     */
    protected final UUID subscriptionId;
    protected final String subscriptionNumber;
    protected final String customerId;
    protected final UUID productId;
    protected final SubscriptionStatus status;
    protected final LocalDate startDate;
    protected final LocalDate endDate;
    protected final BigDecimal price;
    protected final String currency;
    protected final LocalDateTime occurredAt;

    protected SubscriptionEvent(
            String eventType,
            SubscriptionEntity subscription,
            SubscriptionStatus previousStatus
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:subscription:" + subscription.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.subscriptionId = subscription.getId();
        this.subscriptionNumber = subscription.getSubscriptionNumber();
        this.customerId = subscription.getCustomer().getId().toString();
        this.productId = subscription.getProduct().getId();
        this.status = subscription.getStatus();
        this.startDate = subscription.getStartDate();
        this.endDate = subscription.getEndDate();
        this.price = subscription.getPrice();
        this.currency = subscription.getCurrency();
        this.occurredAt = LocalDateTime.now();
    }

    // Getters for CloudEvents required fields
    public String getId() { return id; }
    public String getSource() { return source; }
    public String getType() { return type; }
    public String getSpecversion() { return specversion; }
    public String getDatacontenttype() { return datacontenttype; }
    public LocalDateTime getTime() { return time; }

    // Getters for event data
    public UUID getSubscriptionId() { return subscriptionId; }
    public String getSubscriptionNumber() { return subscriptionNumber; }
    public String getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new subscription is created
 */
class SubscriptionCreatedEvent extends SubscriptionEvent {

    public SubscriptionCreatedEvent(SubscriptionEntity subscription) {
        super("com.droid.bss.subscription.created.v1", subscription, null);
    }
}

/**
 * Event fired when subscription is renewed
 */
class SubscriptionRenewedEvent extends SubscriptionEvent {

    private final LocalDate newEndDate;
    private final LocalDate nextBillingDate;

    public SubscriptionRenewedEvent(SubscriptionEntity subscription) {
        super("com.droid.bss.subscription.renewed.v1", subscription, null);
        this.newEndDate = subscription.getEndDate();
        this.nextBillingDate = subscription.getNextBillingDate();
    }

    public LocalDate getNewEndDate() { return newEndDate; }
    public LocalDate getNextBillingDate() { return nextBillingDate; }
}

/**
 * Event fired when subscription is suspended
 */
class SubscriptionSuspendedEvent extends SubscriptionEvent {

    private final String suspensionReason;

    public SubscriptionSuspendedEvent(SubscriptionEntity subscription, String suspensionReason) {
        super("com.droid.bss.subscription.suspended.v1", subscription, null);
        this.suspensionReason = suspensionReason;
    }

    public String getSuspensionReason() { return suspensionReason; }
}

/**
 * Event fired when subscription is cancelled
 */
class SubscriptionCancelledEvent extends SubscriptionEvent {

    private final String cancellationReason;

    public SubscriptionCancelledEvent(SubscriptionEntity subscription, String cancellationReason) {
        super("com.droid.bss.subscription.cancelled.v1", subscription, null);
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationReason() { return cancellationReason; }
}

/**
 * Event fired when subscription expires
 */
class SubscriptionExpiredEvent extends SubscriptionEvent {

    private final LocalDate expiredDate;

    public SubscriptionExpiredEvent(SubscriptionEntity subscription) {
        super("com.droid.bss.subscription.expired.v1", subscription, null);
        this.expiredDate = subscription.getEndDate();
    }

    public LocalDate getExpiredDate() { return expiredDate; }
}
