package com.droid.bss.domain.payment;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.invoice.InvoiceId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment aggregate root
 * Manages payment processing and tracking
 */
public class Payment {

    private final PaymentId id;
    private final String paymentNumber;
    private final CustomerId customerId;
    private final InvoiceId invoiceId;
    private final BigDecimal amount;
    private final String currency;
    private final PaymentMethod paymentMethod;
    private final PaymentStatus status;
    private final String transactionId;
    private final String gateway;
    private final LocalDate paymentDate;
    private final LocalDate receivedDate;
    private final String referenceNumber;
    private final String notes;
    private final String reversalReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Payment(
            PaymentId id,
            String paymentNumber,
            CustomerId customerId,
            InvoiceId invoiceId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String transactionId,
            String gateway,
            LocalDate paymentDate,
            LocalDate receivedDate,
            String referenceNumber,
            String notes,
            String reversalReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "Payment ID cannot be null");
        this.paymentNumber = Objects.requireNonNull(paymentNumber, "Payment number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.invoiceId = invoiceId; // Optional - payments can be without invoice
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.currency = currency != null ? currency : "PLN";
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.transactionId = transactionId;
        this.gateway = gateway;
        this.paymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        this.receivedDate = receivedDate;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
        this.reversalReason = reversalReason;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated date cannot be null");
        this.version = version;
    }

    /**
     * Creates a new Payment for an invoice
     */
    public static Payment createForInvoice(
            String paymentNumber,
            CustomerId customerId,
            InvoiceId invoiceId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String transactionId,
            String gateway
    ) {
        return create(
            paymentNumber,
            customerId,
            invoiceId,
            amount,
            "PLN",
            paymentMethod,
            transactionId,
            gateway,
            null,
            null
        );
    }

    /**
     * Creates a new standalone Payment (not for specific invoice)
     */
    public static Payment create(
            String paymentNumber,
            CustomerId customerId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String transactionId
    ) {
        return create(
            paymentNumber,
            customerId,
            null,
            amount,
            "PLN",
            paymentMethod,
            transactionId,
            null,
            null,
            null
        );
    }

    /**
     * Creates a new Payment with all parameters
     */
    public static Payment create(
            String paymentNumber,
            CustomerId customerId,
            InvoiceId invoiceId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            String transactionId,
            String gateway,
            String referenceNumber,
            String notes
    ) {
        Objects.requireNonNull(paymentNumber, "Payment number cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Objects.requireNonNull(paymentMethod, "Payment method cannot be null");

        PaymentId paymentId = PaymentId.generate();
        LocalDateTime now = LocalDateTime.now();

        return new Payment(
            paymentId,
            paymentNumber,
            customerId,
            invoiceId,
            amount,
            currency,
            paymentMethod,
            PaymentStatus.PENDING,
            transactionId,
            gateway,
            LocalDate.now(),
            null,
            referenceNumber,
            notes,
            null,
            now,
            now,
            1
        );
    }

    /**
     * Updates payment status (immutable operation)
     */
    public Payment changeStatus(PaymentStatus newStatus) {
        validateStatusTransition(this.status, newStatus);

        LocalDate receivedDate = (newStatus == PaymentStatus.COMPLETED && this.receivedDate == null)
            ? LocalDate.now()
            : this.receivedDate;
        LocalDateTime now = LocalDateTime.now();

        return new Payment(
            this.id,
            this.paymentNumber,
            this.customerId,
            this.invoiceId,
            this.amount,
            this.currency,
            this.paymentMethod,
            newStatus,
            this.transactionId,
            this.gateway,
            this.paymentDate,
            receivedDate,
            this.referenceNumber,
            this.notes,
            this.reversalReason,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Marks payment as completed (immutable operation)
     */
    public Payment complete() {
        return changeStatus(PaymentStatus.COMPLETED);
    }

    /**
     * Marks payment as failed (immutable operation)
     */
    public Payment fail() {
        return changeStatus(PaymentStatus.FAILED);
    }

    /**
     * Refunds payment (immutable operation)
     */
    public Payment refund(String reason) {
        if (this.status != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only completed payments can be refunded");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Payment(
            this.id,
            this.paymentNumber,
            this.customerId,
            this.invoiceId,
            this.amount,
            this.currency,
            this.paymentMethod,
            PaymentStatus.REFUNDED,
            this.transactionId,
            this.gateway,
            this.paymentDate,
            this.receivedDate,
            this.referenceNumber,
            this.notes,
            reason,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Adds notes to payment (immutable operation)
     */
    public Payment addNotes(String newNotes) {
        LocalDateTime now = LocalDateTime.now();

        return new Payment(
            this.id,
            this.paymentNumber,
            this.customerId,
            this.invoiceId,
            this.amount,
            this.currency,
            this.paymentMethod,
            this.status,
            this.transactionId,
            this.gateway,
            this.paymentDate,
            this.receivedDate,
            this.referenceNumber,
            newNotes,
            this.reversalReason,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Validates status transition
     */
    private void validateStatusTransition(PaymentStatus from, PaymentStatus to) {
        switch (from) {
            case PENDING:
                if (to != PaymentStatus.PROCESSING && to != PaymentStatus.COMPLETED &&
                    to != PaymentStatus.FAILED && to != PaymentStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from PENDING to " + to);
                }
                break;
            case PROCESSING:
                if (to != PaymentStatus.COMPLETED && to != PaymentStatus.FAILED) {
                    throw new IllegalArgumentException("Invalid transition from PROCESSING to " + to);
                }
                break;
            case COMPLETED:
            case REFUNDED:
                throw new IllegalArgumentException("Cannot transition from " + from);
            case FAILED:
                if (to != PaymentStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from FAILED to " + to);
                }
                break;
            case CANCELLED:
                throw new IllegalArgumentException("Cannot transition from CANCELLED");
        }
    }

    // Business methods
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isCancelled() {
        return status == PaymentStatus.CANCELLED;
    }

    public boolean isForInvoice() {
        return invoiceId != null;
    }

    public boolean canBeModified() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED;
    }

    // Getters
    public PaymentId getId() {
        return id;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public InvoiceId getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getGateway() {
        return gateway;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    // Property-style accessors (for mapping convenience)
    public PaymentId id() { return id; }
    public String paymentNumber() { return paymentNumber; }
    public CustomerId customerId() { return customerId; }
    public InvoiceId invoiceId() { return invoiceId; }
    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
    public PaymentMethod paymentMethod() { return paymentMethod; }
    public PaymentStatus status() { return status; }
    public String transactionId() { return transactionId; }
    public String gateway() { return gateway; }
    public LocalDate paymentDate() { return paymentDate; }
    public LocalDate receivedDate() { return receivedDate; }
    public String referenceNumber() { return referenceNumber; }
    public String notes() { return notes; }
    public String reversalReason() { return reversalReason; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
    public int version() { return version; }

    /**
     * Restores Payment from persistence state (for infrastructure layer)
     * Package-private - use by repository implementations only
     */
    static Payment restore(
            UUID id,
            String paymentNumber,
            UUID customerId,
            UUID invoiceId,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            String transactionId,
            String gateway,
            LocalDate paymentDate,
            LocalDate receivedDate,
            String referenceNumber,
            String notes,
            String reversalReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        return new Payment(
            new PaymentId(id),
            paymentNumber,
            new CustomerId(customerId),
            invoiceId != null ? new InvoiceId(invoiceId) : null,
            amount,
            currency,
            paymentMethod,
            status,
            transactionId,
            gateway,
            paymentDate,
            receivedDate,
            referenceNumber,
            notes,
            reversalReason,
            createdAt,
            updatedAt,
            version
        );
    }
}
