package com.droid.bss.domain.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payment reconciliation item for tracking individual payment matches
 */
@Entity
@Table(name = "payment_reconciliation_items")
public class PaymentReconciliationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_id")
    private PaymentReconciliationEntity reconciliation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private PaymentEntity payment;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "payment_amount", precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "bank_reference", length = 100)
    private String bankReference;

    @Column(name = "bank_amount", precision = 12, scale = 2)
    private BigDecimal bankAmount;

    @Column(name = "bank_date")
    private LocalDate bankDate;

    @Column(name = "discrepancy_amount", precision = 12, scale = 2)
    private BigDecimal discrepancyAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ReconciliationItemStatus status = ReconciliationItemStatus.UNMATCHED;

    @Column(name = "matched_at")
    private java.time.LocalDateTime matchedAt;

    @Column(name = "match_criteria", columnDefinition = "TEXT")
    private String matchCriteria;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public PaymentReconciliationItem() {
    }

    public PaymentReconciliationItem(PaymentEntity payment) {
        this.payment = payment;
        this.paymentReference = payment.getPaymentNumber();
        this.paymentAmount = payment.getAmount();
        this.paymentDate = payment.getPaymentDate();
        this.status = ReconciliationItemStatus.UNMATCHED;
    }

    public void matchWithBank(String bankReference, BigDecimal bankAmount, LocalDate bankDate) {
        this.bankReference = bankReference;
        this.bankAmount = bankAmount;
        this.bankDate = bankDate;
        this.matchedAt = java.time.LocalDateTime.now();

        // Calculate discrepancy
        if (this.paymentAmount != null && bankAmount != null) {
            this.discrepancyAmount = this.paymentAmount.subtract(bankAmount).abs();

            // Determine status based on discrepancy
            if (this.discrepancyAmount.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                // Amounts match (within 1 cent)
                this.status = ReconciliationItemStatus.MATCHED;
                this.matchCriteria = "Amount and date match";
            } else {
                // Amount discrepancy
                this.status = ReconciliationItemStatus.DISCREPANCY;
                this.matchCriteria = "Amount mismatch";
            }
        }
    }

    public void markAsUnmatched() {
        this.status = ReconciliationItemStatus.UNMATCHED;
        this.bankReference = null;
        this.bankAmount = null;
        this.bankDate = null;
        this.discrepancyAmount = null;
        this.matchedAt = null;
        this.matchCriteria = null;
    }

    public void resolveDiscrepancy(String resolution) {
        if (this.status == ReconciliationItemStatus.DISCREPANCY) {
            this.status = ReconciliationItemStatus.RESOLVED;
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Resolution: " + resolution;
        }
    }

    public boolean isMatched() {
        return status == ReconciliationItemStatus.MATCHED;
    }

    public boolean hasDiscrepancy() {
        return status == ReconciliationItemStatus.DISCREPANCY;
    }

    public boolean isUnmatched() {
        return status == ReconciliationItemStatus.UNMATCHED;
    }

    public boolean isResolved() {
        return status == ReconciliationItemStatus.RESOLVED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentReconciliationEntity getReconciliation() {
        return reconciliation;
    }

    public void setReconciliation(PaymentReconciliationEntity reconciliation) {
        this.reconciliation = reconciliation;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public void setPayment(PaymentEntity payment) {
        this.payment = payment;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getBankReference() {
        return bankReference;
    }

    public void setBankReference(String bankReference) {
        this.bankReference = bankReference;
    }

    public BigDecimal getBankAmount() {
        return bankAmount;
    }

    public void setBankAmount(BigDecimal bankAmount) {
        this.bankAmount = bankAmount;
    }

    public LocalDate getBankDate() {
        return bankDate;
    }

    public void setBankDate(LocalDate bankDate) {
        this.bankDate = bankDate;
    }

    public BigDecimal getDiscrepancyAmount() {
        return discrepancyAmount;
    }

    public void setDiscrepancyAmount(BigDecimal discrepancyAmount) {
        this.discrepancyAmount = discrepancyAmount;
    }

    public ReconciliationItemStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationItemStatus status) {
        this.status = status;
    }

    public java.time.LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(java.time.LocalDateTime matchedAt) {
        this.matchedAt = matchedAt;
    }

    public String getMatchCriteria() {
        return matchCriteria;
    }

    public void setMatchCriteria(String matchCriteria) {
        this.matchCriteria = matchCriteria;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
