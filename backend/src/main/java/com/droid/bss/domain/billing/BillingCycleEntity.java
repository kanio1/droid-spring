package com.droid.bss.domain.billing;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.invoice.InvoiceEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Billing cycles for recurring charges
 */
@Entity
@Table(name = "billing_cycles")
public class BillingCycleEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @NotNull
    @Column(name = "cycle_start", nullable = false)
    private LocalDate cycleStart;

    @NotNull
    @Column(name = "cycle_end", nullable = false)
    private LocalDate cycleEnd;

    @NotNull
    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", nullable = false)
    private BillingCycleType cycleType;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private java.math.BigDecimal taxAmount;

    @Column(name = "total_with_tax", precision = 10, scale = 2)
    private java.math.BigDecimal totalWithTax;

    @Column(name = "status")
    private BillingCycleStatus status = BillingCycleStatus.PENDING;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "invoice_count")
    private Integer invoiceCount = 0;

    @OneToMany(mappedBy = "billingCycle", cascade = CascadeType.ALL)
    private List<InvoiceEntity> invoices = new ArrayList<>();

    protected BillingCycleEntity() {
    }

    public BillingCycleEntity(
            CustomerEntity customer,
            LocalDate cycleStart,
            LocalDate cycleEnd,
            LocalDate billingDate,
            BillingCycleType cycleType) {
        this.customer = customer;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.billingDate = billingDate;
        this.cycleType = cycleType;
    }

    // Getters and Setters
    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public LocalDate getCycleStart() {
        return cycleStart;
    }

    public void setCycleStart(LocalDate cycleStart) {
        this.cycleStart = cycleStart;
    }

    public LocalDate getCycleEnd() {
        return cycleEnd;
    }

    public void setCycleEnd(LocalDate cycleEnd) {
        this.cycleEnd = cycleEnd;
    }

    public LocalDate getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDate billingDate) {
        this.billingDate = billingDate;
    }

    public BillingCycleType getCycleType() {
        return cycleType;
    }

    public void setCycleType(BillingCycleType cycleType) {
        this.cycleType = cycleType;
    }

    public java.math.BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(java.math.BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public java.math.BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(java.math.BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public java.math.BigDecimal getTotalWithTax() {
        return totalWithTax;
    }

    public void setTotalWithTax(java.math.BigDecimal totalWithTax) {
        this.totalWithTax = totalWithTax;
    }

    public BillingCycleStatus getStatus() {
        return status;
    }

    public void setStatus(BillingCycleStatus status) {
        this.status = status;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Integer getInvoiceCount() {
        return invoiceCount;
    }

    public void setInvoiceCount(Integer invoiceCount) {
        this.invoiceCount = invoiceCount;
    }

    public List<InvoiceEntity> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<InvoiceEntity> invoices) {
        this.invoices = invoices;
    }

    // Business methods
    public void addInvoice(InvoiceEntity invoice) {
        this.invoices.add(invoice);
        invoice.setBillingCycle(this);
        this.invoiceCount = this.invoices.size();
    }

    public void removeInvoice(InvoiceEntity invoice) {
        this.invoices.remove(invoice);
        invoice.setBillingCycle(null);
        this.invoiceCount = this.invoices.size();
    }

    public boolean isPending() {
        return this.status == BillingCycleStatus.PENDING;
    }

    public boolean isGenerated() {
        return this.status == BillingCycleStatus.GENERATED;
    }

    public boolean isProcessed() {
        return this.status == BillingCycleStatus.PROCESSED;
    }
}
