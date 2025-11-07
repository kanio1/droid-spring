package com.droid.bss.domain.partner;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Partner settlement for tracking commission payments
 */
@Entity
@Table(name = "partner_settlements")
public class PartnerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private PartnerEntity partner;

    @NotNull
    @Column(name = "settlement_number", unique = true, length = 100)
    private String settlementNumber;

    @NotNull
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @NotNull
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @NotNull
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private SettlementStatus status = SettlementStatus.PENDING;

    @NotNull
    @Column(name = "total_sales", precision = 15, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @NotNull
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @NotNull
    @Column(name = "gross_commission", precision = 15, scale = 2)
    private BigDecimal grossCommission = BigDecimal.ZERO;

    @Column(name = "deductions", precision = 15, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @NotNull
    @Column(name = "net_commission", precision = 15, scale = 2)
    private BigDecimal netCommission = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // BANK_TRANSFER, CHECK, WIRE

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "processed_by", length = 255)
    private String processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Transaction details
    @Column(name = "transactions_count")
    private Long transactionsCount = 0L;

    @Column(name = "products_sold", columnDefinition = "TEXT")
    private String productsSold;

    public PartnerSettlement() {}

    public PartnerSettlement(PartnerEntity partner, LocalDate periodStart, LocalDate periodEnd) {
        this.partner = partner;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.settlementDate = LocalDate.now();
        this.settlementNumber = generateSettlementNumber();
    }

    public static String generateSettlementNumber() {
        return "SET-" + LocalDate.now().getYear() + "-" + System.currentTimeMillis();
    }

    public void process(String processedBy) {
        this.status = SettlementStatus.PROCESSED;
        this.processedBy = processedBy;
        this.processedAt = LocalDateTime.now();
    }

    public void pay(BigDecimal amount, String paymentMethod, String paymentReference) {
        this.status = SettlementStatus.PAID;
        this.paidAmount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.paidDate = LocalDate.now();

        // Update partner balance
        if (this.partner != null) {
            this.partner.processSettlement(amount);
        }
    }

    public void reverse(String reason) {
        this.status = SettlementStatus.REVERSED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Reversed: " + reason + " on " + LocalDate.now();
    }

    public boolean isPaid() {
        return status == SettlementStatus.PAID;
    }

    public boolean isPending() {
        return status == SettlementStatus.PENDING;
    }

    public boolean isProcessed() {
        return status == SettlementStatus.PROCESSED;
    }

    public boolean isReversed() {
        return status == SettlementStatus.REVERSED;
    }

    public BigDecimal calculateNetCommission() {
        this.netCommission = this.grossCommission.subtract(this.deductions);
        return this.netCommission;
    }

    public void addTransaction(BigDecimal saleAmount, BigDecimal commissionRate) {
        this.totalSales = this.totalSales.add(saleAmount);
        this.transactionsCount = (this.transactionsCount != null ? this.transactionsCount : 0L) + 1;

        // Calculate commission
        BigDecimal commission = saleAmount.multiply(commissionRate).divide(BigDecimal.valueOf(100));
        this.grossCommission = this.grossCommission.add(commission);
    }

    public void addDeduction(BigDecimal amount, String reason) {
        this.deductions = this.deductions.add(amount);
        calculateNetCommission();
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Deduction: " + reason + " (" + amount + ")";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PartnerEntity getPartner() {
        return partner;
    }

    public void setPartner(PartnerEntity partner) {
        this.partner = partner;
    }

    public String getSettlementNumber() {
        return settlementNumber;
    }

    public void setSettlementNumber(String settlementNumber) {
        this.settlementNumber = settlementNumber;
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

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getGrossCommission() {
        return grossCommission;
    }

    public void setGrossCommission(BigDecimal grossCommission) {
        this.grossCommission = grossCommission;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetCommission() {
        return netCommission;
    }

    public void setNetCommission(BigDecimal netCommission) {
        this.netCommission = netCommission;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getTransactionsCount() {
        return transactionsCount;
    }

    public void setTransactionsCount(Long transactionsCount) {
        this.transactionsCount = transactionsCount;
    }

    public String getProductsSold() {
        return productsSold;
    }

    public void setProductsSold(String productsSold) {
        this.productsSold = productsSold;
    }
}
