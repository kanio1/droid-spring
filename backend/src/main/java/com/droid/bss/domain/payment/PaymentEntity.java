package com.droid.bss.domain.payment;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.invoice.InvoiceEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment entity for payment tracking
 *
 * NOTE: In production, sensitive payment data (card numbers, etc.) should be
 * encrypted at rest using JPA converters or database-level encryption.
 */
@Entity
@Table(name = "payments")
@SQLRestriction("deleted_at IS NULL")
public class PaymentEntity extends BaseEntity {

    @Column(name = "payment_number", nullable = false, unique = true, length = 50)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "gateway", length = 50)
    private String gateway;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    // Constructors
    public PaymentEntity() {}

    public PaymentEntity(
            String paymentNumber,
            CustomerEntity customer,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            LocalDate paymentDate
    ) {
        this.paymentNumber = paymentNumber;
        this.customer = customer;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    public PaymentEntity(
            String paymentNumber,
            CustomerEntity customer,
            InvoiceEntity invoice,
            BigDecimal amount,
            String currency,
            PaymentMethod paymentMethod,
            PaymentStatus paymentStatus,
            LocalDate paymentDate
    ) {
        this.paymentNumber = paymentNumber;
        this.customer = customer;
        this.invoice = invoice;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
    }

    // Business methods
    public boolean isPending() {
        return paymentStatus == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return paymentStatus == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return paymentStatus == PaymentStatus.REFUNDED;
    }

    public boolean canBeRefunded() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }

    public boolean canBeRetried() {
        return paymentStatus == PaymentStatus.FAILED;
    }

    public void markAsCompleted() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.receivedDate = LocalDate.now();
    }

    public void markAsFailed(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.reversalReason = reason;
    }

    public void markAsRefunded(String reason) {
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.reversalReason = reason;
    }

    public void process() {
        this.paymentStatus = PaymentStatus.PROCESSING;
    }

    public void complete() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.receivedDate = LocalDate.now();
    }

    // Soft delete methods
    public void markAsDeleted() {
        this.deletedAt = LocalDate.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // Getters and setters
    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * Converts JPA entity to DDD aggregate
     */
    public Payment toDomain() {
        return Payment.restore(
            this.id,
            this.paymentNumber,
            this.customer != null ? this.customer.getId() : null,
            this.invoice != null ? this.invoice.getId() : null,
            this.amount,
            this.currency,
            this.paymentMethod,
            this.paymentStatus,
            this.transactionId,
            this.gateway,
            this.paymentDate,
            this.receivedDate,
            this.referenceNumber,
            this.notes,
            this.reversalReason,
            this.createdAt,
            this.updatedAt,
            this.version != null ? this.version.intValue() : 0
        );
    }

    /**
     * Creates JPA entity from DDD aggregate
     */
    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.id().value();
        entity.paymentNumber = payment.paymentNumber();
        // Note: Customer and Invoice need to be set by repository
        entity.amount = payment.amount();
        entity.currency = payment.currency();
        entity.paymentMethod = payment.paymentMethod();
        entity.paymentStatus = payment.status();
        entity.transactionId = payment.transactionId();
        entity.gateway = payment.gateway();
        entity.paymentDate = payment.paymentDate();
        entity.receivedDate = payment.receivedDate();
        entity.referenceNumber = payment.referenceNumber();
        entity.notes = payment.notes();
        entity.reversalReason = payment.reversalReason();
        entity.createdAt = payment.createdAt();
        entity.updatedAt = payment.updatedAt();
        // Note: deletedAt should be set separately for soft delete
        return entity;
    }
}
