package com.droid.bss.domain.payment.event;

import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Payment-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class PaymentEvent {

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
    protected final UUID paymentId;
    protected final String paymentNumber;
    protected final String customerId;
    protected final String invoiceId;
    protected final BigDecimal amount;
    protected final String currency;
    protected final PaymentMethod paymentMethod;
    protected final PaymentStatus status;
    protected final LocalDateTime occurredAt;

    protected PaymentEvent(
            String eventType,
            PaymentEntity payment,
            PaymentStatus previousStatus
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:payment:" + payment.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.paymentId = payment.getId();
        this.paymentNumber = payment.getPaymentNumber();
        this.customerId = payment.getCustomer().getId().toString();
        this.invoiceId = payment.getInvoice() != null ? payment.getInvoice().getId().toString() : null;
        this.amount = payment.getAmount();
        this.currency = payment.getCurrency();
        this.paymentMethod = payment.getPaymentMethod();
        this.status = payment.getPaymentStatus();
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
    public UUID getPaymentId() { return paymentId; }
    public String getPaymentNumber() { return paymentNumber; }
    public String getCustomerId() { return customerId; }
    public String getInvoiceId() { return invoiceId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new payment is created
 */
class PaymentCreatedEvent extends PaymentEvent {

    public PaymentCreatedEvent(PaymentEntity payment) {
        super("com.droid.bss.payment.created.v1", payment, null);
    }
}

/**
 * Event fired when payment processing starts
 */
class PaymentProcessingEvent extends PaymentEvent {

    private final String transactionId;

    public PaymentProcessingEvent(PaymentEntity payment) {
        super("com.droid.bss.payment.processing.v1", payment, null);
        this.transactionId = payment.getTransactionId();
    }

    public String getTransactionId() { return transactionId; }
}

/**
 * Event fired when payment is completed successfully
 */
class PaymentCompletedEvent extends PaymentEvent {

    private final LocalDate receivedDate;

    public PaymentCompletedEvent(PaymentEntity payment) {
        super("com.droid.bss.payment.completed.v1", payment, null);
        this.receivedDate = payment.getReceivedDate();
    }

    public LocalDate getReceivedDate() { return receivedDate; }
}

/**
 * Event fired when payment fails
 */
class PaymentFailedEvent extends PaymentEvent {

    private final String failureReason;

    public PaymentFailedEvent(PaymentEntity payment, String failureReason) {
        super("com.droid.bss.payment.failed.v1", payment, null);
        this.failureReason = failureReason;
    }

    public String getFailureReason() { return failureReason; }
}

/**
 * Event fired when payment is refunded
 */
class PaymentRefundedEvent extends PaymentEvent {

    private final String refundReason;

    public PaymentRefundedEvent(PaymentEntity payment, String refundReason) {
        super("com.droid.bss.payment.refunded.v1", payment, null);
        this.refundReason = refundReason;
    }

    public String getRefundReason() { return refundReason; }
}
