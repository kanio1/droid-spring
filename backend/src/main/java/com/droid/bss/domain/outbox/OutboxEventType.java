package com.droid.bss.domain.outbox;

/**
 * Outbox Event Types
 *
 * Enumerates all possible event types in the outbox pattern
 */
public enum OutboxEventType {

    // Customer Events
    CUSTOMER_CREATED("CustomerCreated"),
    CUSTOMER_UPDATED("CustomerUpdated"),
    CUSTOMER_DELETED("CustomerDeleted"),

    // Address Events
    ADDRESS_CREATED("AddressCreated"),
    ADDRESS_UPDATED("AddressUpdated"),
    ADDRESS_DELETED("AddressDeleted"),
    ADDRESS_SET_PRIMARY("AddressSetPrimary"),

    // Product Events
    PRODUCT_CREATED("ProductCreated"),
    PRODUCT_UPDATED("ProductUpdated"),
    PRODUCT_DELETED("ProductDeleted"),
    PRODUCT_ACTIVATED("ProductActivated"),
    PRODUCT_DEACTIVATED("ProductDeactivated"),

    // Order Events
    ORDER_CREATED("OrderCreated"),
    ORDER_UPDATED("OrderUpdated"),
    ORDER_CANCELED("OrderCanceled"),
    ORDER_COMPLETED("OrderCompleted"),
    ORDER_PROCESSING_STARTED("OrderProcessingStarted"),

    // Subscription Events
    SUBSCRIPTION_CREATED("SubscriptionCreated"),
    SUBSCRIPTION_UPDATED("SubscriptionUpdated"),
    SUBSCRIPTION_CANCELED("SubscriptionCanceled"),
    SUBSCRIPTION_ACTIVATED("SubscriptionActivated"),
    SUBSCRIPTION_RENEWED("SubscriptionRenewed"),
    SUBSCRIPTION_SUSPENDED("SubscriptionSuspended"),

    // Payment Events
    PAYMENT_CREATED("PaymentCreated"),
    PAYMENT_PROCESSED("PaymentProcessed"),
    PAYMENT_REFUNDED("PaymentRefunded"),
    PAYMENT_REVERSED("PaymentReversed"),
    PAYMENT_FAILED("PaymentFailed"),

    // Invoice Events
    INVOICE_CREATED("InvoiceCreated"),
    INVOICE_SENT("InvoiceSent"),
    INVOICE_PAID("InvoicePaid"),
    INVOICE_OVERDUE("InvoiceOverdue"),
    INVOICE_CANCELED("InvoiceCanceled"),

    // Billing Events
    USAGE_RECORDED("UsageRecorded"),
    INVOICE_GENERATED("InvoiceGenerated"),
    BILLING_CYCLE_STARTED("BillingCycleStarted"),
    BILLING_CYCLE_COMPLETED("BillingCycleCompleted"),

    // System Events
    SYSTEM_STATUS_CHANGED("SystemStatusChanged"),
    CONFIGURATION_UPDATED("ConfigurationUpdated"),
    INTEGRATION_EVENT("IntegrationEvent");

    private final String eventName;

    OutboxEventType(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    /**
     * Get enum by event name
     */
    public static OutboxEventType fromEventName(String eventName) {
        for (OutboxEventType type : values()) {
            if (type.eventName.equals(eventName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown event type: " + eventName);
    }
}
