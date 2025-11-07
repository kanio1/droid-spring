package com.droid.bss.domain.payment;

import com.droid.bss.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment reconciliation entity for matching payments with bank statements
 */
@Entity
@Table(name = "payment_reconciliations")
public class PaymentReconciliationEntity extends BaseEntity {

    @NotNull
    @Column(name = "reconciliation_id", unique = true, length = 100)
    private String reconciliationId;

    @NotNull
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    @Column(name = "reconciliation_date")
    private LocalDate reconciliationDate;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    // Summary totals
    @Column(name = "total_payments", precision = 15, scale = 2)
    private BigDecimal totalPayments = BigDecimal.ZERO;

    @Column(name = "total_receipts", precision = 15, scale = 2)
    private BigDecimal totalReceipts = BigDecimal.ZERO;

    @Column(name = "total_discrepancies", precision = 15, scale = 2)
    private BigDecimal totalDiscrepancies = BigDecimal.ZERO;

    @Column(name = "matched_amount", precision = 15, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Column(name = "unmatched_amount", precision = 15, scale = 2)
    private BigDecimal unmatchedAmount = BigDecimal.ZERO;

    @Column(name = "discrepancy_count")
    private Long discrepancyCount = 0L;

    // Reference data
    @Column(name = "bank_statement_file", length = 500)
    private String bankStatementFile;

    @Column(name = "gateway_name", length = 100)
    private String gatewayName; // STRIPE, PAYPAL, BANK, etc.

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Payment counts
    @Column(name = "payments_count")
    private Long paymentsCount = 0L;

    @Column(name = "matched_payments_count")
    private Long matchedPaymentsCount = 0L;

    @Column(name = "unmatched_payments_count")
    private Long unmatchedPaymentsCount = 0L;

    @Column(name = "failed_payments_count")
    private Long failedPaymentsCount = 0L;

    @OneToMany(mappedBy = "reconciliation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentReconciliationItem> items = new ArrayList<>();

    public PaymentReconciliationEntity() {
    }

    public PaymentReconciliationEntity(String reconciliationId, LocalDate periodStart, LocalDate periodEnd, String performedBy) {
        this.reconciliationId = reconciliationId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.performedBy = performedBy;
        this.reconciliationDate = LocalDate.now();
    }

    // Business methods
    public void addItem(PaymentReconciliationItem item) {
        items.add(item);
        item.setReconciliation(this);
        updateSummary();
    }

    public void removeItem(PaymentReconciliationItem item) {
        items.remove(item);
        item.setReconciliation(null);
        updateSummary();
    }

    public void updateSummary() {
        this.totalDiscrepancies = items.stream()
                .map(PaymentReconciliationItem::getDiscrepancyAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.matchedAmount = items.stream()
                .filter(item -> item.getStatus() == ReconciliationItemStatus.MATCHED)
                .map(PaymentReconciliationItem::getPaymentAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.unmatchedAmount = items.stream()
                .filter(item -> item.getStatus() == ReconciliationItemStatus.UNMATCHED)
                .map(PaymentReconciliationItem::getPaymentAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.matchedPaymentsCount = items.stream()
                .filter(item -> item.getStatus() == ReconciliationItemStatus.MATCHED)
                .count();

        this.unmatchedPaymentsCount = items.stream()
                .filter(item -> item.getStatus() == ReconciliationItemStatus.UNMATCHED)
                .count();

        this.discrepancyCount = items.stream()
                .filter(item -> item.getStatus() == ReconciliationItemStatus.DISCREPANCY)
                .count();
    }

    public void startReconciliation() {
        this.status = ReconciliationStatus.IN_PROGRESS;
    }

    public void completeReconciliation() {
        this.status = ReconciliationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsDiscrepancy() {
        this.status = ReconciliationStatus.DISCREPANCY;
    }

    public boolean isCompleted() {
        return status == ReconciliationStatus.COMPLETED;
    }

    public boolean hasDiscrepancies() {
        return discrepancyCount != null && discrepancyCount > 0;
    }

    public double getMatchRate() {
        if (paymentsCount == null || paymentsCount == 0) return 0.0;
        return (double) matchedPaymentsCount / paymentsCount * 100;
    }

    // Getters and Setters
    public String getReconciliationId() {
        return reconciliationId;
    }

    public void setReconciliationId(String reconciliationId) {
        this.reconciliationId = reconciliationId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public LocalDate getReconciliationDate() {
        return reconciliationDate;
    }

    public void setReconciliationDate(LocalDate reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public BigDecimal getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(BigDecimal totalPayments) {
        this.totalPayments = totalPayments;
    }

    public BigDecimal getTotalReceipts() {
        return totalReceipts;
    }

    public void setTotalReceipts(BigDecimal totalReceipts) {
        this.totalReceipts = totalReceipts;
    }

    public BigDecimal getTotalDiscrepancies() {
        return totalDiscrepancies;
    }

    public void setTotalDiscrepancies(BigDecimal totalDiscrepancies) {
        this.totalDiscrepancies = totalDiscrepancies;
    }

    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }

    public void setMatchedAmount(BigDecimal matchedAmount) {
        this.matchedAmount = matchedAmount;
    }

    public BigDecimal getUnmatchedAmount() {
        return unmatchedAmount;
    }

    public void setUnmatchedAmount(BigDecimal unmatchedAmount) {
        this.unmatchedAmount = unmatchedAmount;
    }

    public Long getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(Long discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public String getBankStatementFile() {
        return bankStatementFile;
    }

    public void setBankStatementFile(String bankStatementFile) {
        this.bankStatementFile = bankStatementFile;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getPaymentsCount() {
        return paymentsCount;
    }

    public void setPaymentsCount(Long paymentsCount) {
        this.paymentsCount = paymentsCount;
    }

    public Long getMatchedPaymentsCount() {
        return matchedPaymentsCount;
    }

    public void setMatchedPaymentsCount(Long matchedPaymentsCount) {
        this.matchedPaymentsCount = matchedPaymentsCount;
    }

    public Long getUnmatchedPaymentsCount() {
        return unmatchedPaymentsCount;
    }

    public void setUnmatchedPaymentsCount(Long unmatchedPaymentsCount) {
        this.unmatchedPaymentsCount = unmatchedPaymentsCount;
    }

    public Long getFailedPaymentsCount() {
        return failedPaymentsCount;
    }

    public void setFailedPaymentsCount(Long failedPaymentsCount) {
        this.failedPaymentsCount = failedPaymentsCount;
    }

    public List<PaymentReconciliationItem> getItems() {
        return items;
    }

    public void setItems(List<PaymentReconciliationItem> items) {
        this.items = items;
    }
}
