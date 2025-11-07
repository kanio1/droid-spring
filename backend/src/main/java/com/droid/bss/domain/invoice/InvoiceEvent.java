package com.droid.bss.domain.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Invoice Domain Event
 * Used for broadcasting invoice-related events via RSocket and Kafka
 */
public class InvoiceEvent {

    private final String eventType;
    private final UUID invoiceId;
    private final UUID customerId;
    private final UUID orderId;
    private final BigDecimal totalAmount;
    private final InvoiceStatus status;
    private final LocalDate dueDate;
    private final LocalDateTime timestamp;
    private final Map<String, Object> data;

    public InvoiceEvent(String eventType, UUID invoiceId, UUID customerId, UUID orderId,
                       BigDecimal totalAmount, InvoiceStatus status, LocalDate dueDate,
                       LocalDateTime timestamp, Map<String, Object> data) {
        this.eventType = eventType;
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.dueDate = dueDate;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static InvoiceEvent generated(UUID invoiceId, UUID customerId, UUID orderId,
                                        BigDecimal totalAmount, InvoiceStatus status) {
        return new InvoiceEvent(
            "INVOICE_GENERATED",
            invoiceId,
            customerId,
            orderId,
            totalAmount,
            status,
            LocalDate.now().plusDays(30),
            LocalDateTime.now(),
            Map.of("operation", "generate")
        );
    }

    public static InvoiceEvent paid(UUID invoiceId, UUID customerId, UUID orderId,
                                   BigDecimal totalAmount) {
        return new InvoiceEvent(
            "INVOICE_PAID",
            invoiceId,
            customerId,
            orderId,
            totalAmount,
            InvoiceStatus.PAID,
            null,
            LocalDateTime.now(),
            Map.of("operation", "payment", "paidAt", LocalDateTime.now())
        );
    }

    public static InvoiceEvent overdue(UUID invoiceId, UUID customerId, UUID orderId,
                                      BigDecimal totalAmount) {
        return new InvoiceEvent(
            "INVOICE_OVERDUE",
            invoiceId,
            customerId,
            orderId,
            totalAmount,
            InvoiceStatus.OVERDUE,
            null,
            LocalDateTime.now(),
            Map.of("operation", "overdue")
        );
    }

    // Getters
    public String getEventType() { return eventType; }
    public UUID getInvoiceId() { return invoiceId; }
    public UUID getCustomerId() { return customerId; }
    public UUID getOrderId() { return orderId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}
