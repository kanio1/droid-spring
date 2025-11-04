package com.droid.bss.domain.invoice.event;

import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for Invoice-related CloudEvents
 * Implements CloudEvents v1.0 specification
 */
public abstract class InvoiceEvent {

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
    protected final UUID invoiceId;
    protected final String invoiceNumber;
    protected final String customerId;
    protected final InvoiceStatus status;
    protected final LocalDateTime occurredAt;

    protected InvoiceEvent(
            String eventType,
            InvoiceEntity invoice,
            InvoiceStatus previousStatus
    ) {
        this.id = UUID.randomUUID().toString();
        this.source = "urn:droid:bss:invoice:" + invoice.getId();
        this.type = eventType;
        this.specversion = "1.0";
        this.datacontenttype = "application/json";
        this.time = LocalDateTime.now();

        this.invoiceId = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.customerId = invoice.getCustomer().getId().toString();
        this.status = invoice.getStatus();
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
    public UUID getInvoiceId() { return invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getCustomerId() { return customerId; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}

/**
 * Event fired when a new invoice is created
 */
class InvoiceCreatedEvent extends InvoiceEvent {

    public InvoiceCreatedEvent(InvoiceEntity invoice) {
        super("com.droid.bss.invoice.created.v1", invoice, null);
    }
}

/**
 * Event fired when an invoice is updated
 */
class InvoiceUpdatedEvent extends InvoiceEvent {

    public InvoiceUpdatedEvent(InvoiceEntity invoice) {
        super("com.droid.bss.invoice.updated.v1", invoice, null);
    }
}

/**
 * Event fired when invoice status is changed
 */
class InvoiceStatusChangedEvent extends InvoiceEvent {

    private final InvoiceStatus previousStatus;

    public InvoiceStatusChangedEvent(InvoiceEntity invoice, InvoiceStatus previousStatus) {
        super("com.droid.bss.invoice.statusChanged.v1", invoice, previousStatus);
        this.previousStatus = previousStatus;
    }

    public InvoiceStatus getPreviousStatus() {
        return previousStatus;
    }
}

/**
 * Event fired when invoice is sent
 */
class InvoiceSentEvent extends InvoiceEvent {

    private final String sentToEmail;
    private final LocalDateTime sentAt;

    public InvoiceSentEvent(InvoiceEntity invoice) {
        super("com.droid.bss.invoice.sent.v1", invoice, null);
        this.sentToEmail = invoice.getSentToEmail();
        this.sentAt = invoice.getSentAt();
    }

    public String getSentToEmail() { return sentToEmail; }
    public LocalDateTime getSentAt() { return sentAt; }
}

/**
 * Event fired when invoice is paid
 */
class InvoicePaidEvent extends InvoiceEvent {

    private final java.time.LocalDate paidDate;

    public InvoicePaidEvent(InvoiceEntity invoice) {
        super("com.droid.bss.invoice.paid.v1", invoice, null);
        this.paidDate = invoice.getPaidDate();
    }

    public java.time.LocalDate getPaidDate() { return paidDate; }
}
