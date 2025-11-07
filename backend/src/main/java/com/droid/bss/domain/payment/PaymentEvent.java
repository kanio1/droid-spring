package com.droid.bss.domain.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payment Domain Event
 * Used for broadcasting payment-related events via RSocket and Kafka
 */
public class PaymentEvent {

    private final String eventType;
    private final UUID paymentId;
    private final UUID invoiceId;
    private final UUID customerId;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final String paymentMethod;
    private final LocalDateTime timestamp;
    private final Map<String, Object> data;

    public PaymentEvent(String eventType, UUID paymentId, UUID invoiceId, UUID customerId,
                       BigDecimal amount, PaymentStatus status, String paymentMethod,
                       LocalDateTime timestamp, Map<String, Object> data) {
        this.eventType = eventType;
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static PaymentEvent processed(UUID paymentId, UUID invoiceId, UUID customerId,
                                        BigDecimal amount, PaymentStatus status,
                                        String paymentMethod) {
        return new PaymentEvent(
            "PAYMENT_PROCESSED",
            paymentId,
            invoiceId,
            customerId,
            amount,
            status,
            paymentMethod,
            LocalDateTime.now(),
            Map.of("operation", "process", "processedAt", LocalDateTime.now())
        );
    }

    public static PaymentEvent failed(UUID paymentId, UUID invoiceId, UUID customerId,
                                     BigDecimal amount, String paymentMethod,
                                     String errorMessage) {
        return new PaymentEvent(
            "PAYMENT_FAILED",
            paymentId,
            invoiceId,
            customerId,
            amount,
            PaymentStatus.FAILED,
            paymentMethod,
            LocalDateTime.now(),
            Map.of("operation", "failure", "error", errorMessage)
        );
    }

    public static PaymentEvent refunded(UUID paymentId, UUID invoiceId, UUID customerId,
                                       BigDecimal amount, String paymentMethod) {
        return new PaymentEvent(
            "PAYMENT_REFUNDED",
            paymentId,
            invoiceId,
            customerId,
            amount,
            PaymentStatus.REFUNDED,
            paymentMethod,
            LocalDateTime.now(),
            Map.of("operation", "refund", "refundedAt", LocalDateTime.now())
        );
    }

    // Getters
    public String getEventType() { return eventType; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getInvoiceId() { return invoiceId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}
