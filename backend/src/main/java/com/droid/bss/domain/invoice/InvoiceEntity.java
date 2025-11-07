package com.droid.bss.domain.invoice;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice entity for invoice generation and management
 */
@Entity
@Table(name = "invoices")
@SQLRestriction("deleted_at IS NULL")
public class InvoiceEntity extends BaseEntity {

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType invoiceType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "billing_period_start")
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end")
    private LocalDate billingPeriodEnd;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "billing_date")
    private LocalDate billingDate;

    @Column(name = "total_with_tax", precision = 12, scale = 2)
    private BigDecimal totalWithTax;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "payment_terms")
    private Integer paymentTerms = 14;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "sent_to_email", length = 200)
    private String sentToEmail;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItemEntity> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_cycle_id")
    private com.droid.bss.domain.billing.BillingCycleEntity billingCycle;

    // Constructors
    public InvoiceEntity() {}

    public InvoiceEntity(
            String invoiceNumber,
            CustomerEntity customer,
            InvoiceType invoiceType,
            InvoiceStatus status,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String currency
    ) {
        this.invoiceNumber = invoiceNumber;
        this.customer = customer;
        this.invoiceType = invoiceType;
        this.status = status;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.currency = currency;
        recalculateAmounts();
    }

    // Business methods
    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isOverdue() {
        return status == InvoiceStatus.OVERDUE ||
               (status == InvoiceStatus.SENT && LocalDate.now().isAfter(dueDate));
    }

    public boolean canBeCancelled() {
        return status == InvoiceStatus.DRAFT || status == InvoiceStatus.ISSUED;
    }

    public boolean canBeSent() {
        return status == InvoiceStatus.DRAFT || status == InvoiceStatus.ISSUED;
    }

    public void calculateTax() {
        if (subtotal != null && discountAmount != null) {
            BigDecimal netAmount = subtotal.subtract(discountAmount);
            this.taxAmount = netAmount.multiply(BigDecimal.valueOf(0.23)); // 23% VAT
            recalculateAmounts();
        }
    }

    public void recalculateAmounts() {
        if (subtotal != null && taxAmount != null) {
            BigDecimal netAmount = subtotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
            this.totalAmount = netAmount.add(taxAmount).add(lateFee != null ? lateFee : BigDecimal.ZERO);
        }
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidDate = LocalDate.now();
    }

    public void markAsSent(String email) {
        this.status = InvoiceStatus.SENT;
        this.sentToEmail = email;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsOverdue() {
        if (status == InvoiceStatus.SENT && LocalDate.now().isAfter(dueDate)) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }

    public void applyLateFee(BigDecimal fee) {
        this.lateFee = fee;
        recalculateAmounts();
    }

    public void addItem(InvoiceItemEntity item) {
        items.add(item);
        item.setInvoice(this);
        recalculateFromItems();
    }

    public void removeItem(InvoiceItemEntity item) {
        items.remove(item);
        item.setInvoice(null);
        recalculateFromItems();
    }

    public void recalculateFromItems() {
        this.subtotal = items.stream()
                .map(InvoiceItemEntity::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        calculateTax();
    }

    // Getters and setters
    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public void setInvoiceType(InvoiceType invoiceType) {
        this.invoiceType = invoiceType;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public LocalDate getBillingPeriodStart() {
        return billingPeriodStart;
    }

    public void setBillingPeriodStart(LocalDate billingPeriodStart) {
        this.billingPeriodStart = billingPeriodStart;
    }

    public LocalDate getBillingPeriodEnd() {
        return billingPeriodEnd;
    }

    public void setBillingPeriodEnd(LocalDate billingPeriodEnd) {
        this.billingPeriodEnd = billingPeriodEnd;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
        recalculateAmounts();
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        recalculateAmounts();
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
        recalculateAmounts();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(Integer paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
        recalculateAmounts();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getSentToEmail() {
        return sentToEmail;
    }

    public void setSentToEmail(String sentToEmail) {
        this.sentToEmail = sentToEmail;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<InvoiceItemEntity> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemEntity> items) {
        this.items = items;
    }

    public LocalDate getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDate billingDate) {
        this.billingDate = billingDate;
    }

    public BigDecimal getTotalWithTax() {
        return totalWithTax;
    }

    public void setTotalWithTax(BigDecimal totalWithTax) {
        this.totalWithTax = totalWithTax;
    }

    public com.droid.bss.domain.billing.BillingCycleEntity getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(com.droid.bss.domain.billing.BillingCycleEntity billingCycle) {
        this.billingCycle = billingCycle;
    }

    /**
     * Converts JPA entity to DDD aggregate
     */
    public Invoice toDomain() {
        return Invoice.restore(
            this.getId(),
            this.invoiceNumber,
            this.customer != null ? this.customer.getId() : null,
            this.status,
            this.totalAmount,
            this.currency,
            this.issueDate,
            this.dueDate,
            null, // orderNumber not stored in entity
            null, // salesRepId not stored in entity
            this.notes,
            this.getCreatedAt(),
            this.getUpdatedAt(),
            this.version != null ? this.version.intValue() : 0,
            this.items != null
                ? this.items.stream()
                    .map(InvoiceItemEntity::toDomain)
                    .toList()
                : java.util.Collections.emptyList()
        );
    }

    /**
     * Creates JPA entity from DDD aggregate
     */
    public static InvoiceEntity from(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(invoice.getId().value());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setStatus(invoice.getStatus());
        entity.setTotalAmount(invoice.getTotalAmount());
        entity.setCurrency(invoice.getCurrency());
        entity.setIssueDate(invoice.getInvoiceDate());
        entity.setDueDate(invoice.getDueDate());
        entity.setNotes(invoice.getNotes());
        // Note: customer relationship should be set by caller
        // Note: items relationship should be set by caller
        return entity;
    }
}
