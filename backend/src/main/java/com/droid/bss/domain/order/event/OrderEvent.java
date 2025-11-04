package com.droid.bss.domain.order.event;

import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Order-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class OrderEvent {

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
    protected final UUID orderId;
    protected final String orderNumber;
    protected final String customerId;
    protected final OrderStatus status;
    protected final BigDecimal totalAmount;
    protected final String currency;
    protected final LocalDateTime occurredAt;

    protected OrderEvent(
            String eventType,
            OrderEntity order,
            OrderStatus previousStatus
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:order:" + order.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.customerId = order.getCustomer().getId().toString();
        this.status = order.getStatus();
        this.totalAmount = order.getTotalAmount();
        this.currency = order.getCurrency();
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
    public UUID getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new order is created
 */
class OrderCreatedEvent extends OrderEvent {

    public OrderCreatedEvent(OrderEntity order) {
        super("com.droid.bss.order.created.v1", order, null);
    }
}

/**
 * Event fired when an order is updated
 */
class OrderUpdatedEvent extends OrderEvent {

    public OrderUpdatedEvent(OrderEntity order) {
        super("com.droid.bss.order.updated.v1", order, null);
    }
}

/**
 * Event fired when order status is changed
 */
class OrderStatusChangedEvent extends OrderEvent {

    private final OrderStatus previousStatus;

    public OrderStatusChangedEvent(OrderEntity order, OrderStatus previousStatus) {
        super("com.droid.bss.order.statusChanged.v1", order, previousStatus);
        this.previousStatus = previousStatus;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }
}

/**
 * Event fired when order is completed
 */
class OrderCompletedEvent extends OrderEvent {

    private final LocalDateTime completedDate;

    public OrderCompletedEvent(OrderEntity order) {
        super("com.droid.bss.order.completed.v1", order, null);
        this.completedDate = order.getCompletedDate() != null
            ? order.getCompletedDate().atStartOfDay()
            : LocalDateTime.now();
    }

    public LocalDateTime getCompletedDate() { return completedDate; }
}

/**
 * Event fired when order is cancelled
 */
class OrderCancelledEvent extends OrderEvent {

    private final String cancellationReason;

    public OrderCancelledEvent(OrderEntity order, String cancellationReason) {
        super("com.droid.bss.order.cancelled.v1", order, null);
        this.cancellationReason = cancellationReason;
    }

    public String getCancellationReason() { return cancellationReason; }
}
