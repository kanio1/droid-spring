package com.droid.bss.domain.invoice;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * Invoice item entity for line items within invoices
 */
@Entity
@Table(name = "invoice_items")
public class InvoiceItemEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    @Column(name = "usage_record_id")
    private String usageRecordId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "item_type", nullable = false, length = 20)
    private InvoiceItemType itemType;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(23.00);

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    // Proration support
    @Column(name = "days_in_period")
    private Integer daysInPeriod;

    @Column(name = "days_billed")
    private Integer daysBilled;

    @Column(name = "is_prorated", nullable = false)
    private Boolean isProrated = false;

    @Column(name = "original_unit_price", precision = 10, scale = 2)
    private BigDecimal originalUnitPrice;

    @Column(name = "advance_payment", nullable = false)
    private Boolean advancePayment = false;

    @Column(name = "advance_amount", precision = 10, scale = 2)
    private BigDecimal advanceAmount;

    @Column(name = "billing_scheme", length = 50)
    private String billingScheme; // EQUAL, USAGE_BASED, TIME_BASED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    // Constructors
    public InvoiceItemEntity() {}

    public InvoiceItemEntity(
            InvoiceEntity invoice,
            InvoiceItemType itemType,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal taxRate
    ) {
        this.invoice = invoice;
        this.itemType = itemType;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate;
        this.taxRate = taxRate;
        recalculateAmounts();
    }

    public InvoiceItemEntity(
            InvoiceEntity invoice,
            SubscriptionEntity subscription,
            InvoiceItemType itemType,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal taxRate
    ) {
        this.invoice = invoice;
        this.subscription = subscription;
        this.itemType = itemType;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountRate = discountRate;
        this.taxRate = taxRate;
        recalculateAmounts();
    }

    // Business methods

    /**
     * Get total price (quantity * unit price)
     */
    public BigDecimal getTotalPrice() {
        if (unitPrice != null && quantity != null && quantity.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return unitPrice.multiply(quantity);
        }
        return java.math.BigDecimal.ZERO;
    }

    public void recalculateAmounts() {
        if (unitPrice != null && quantity != null && quantity.compareTo(java.math.BigDecimal.ZERO) > 0) {
            BigDecimal totalPrice = getTotalPrice();

            // Calculate discount
            if (discountRate != null && discountRate.compareTo(java.math.BigDecimal.ZERO) > 0) {
                this.discountAmount = totalPrice.multiply(discountRate).divide(java.math.BigDecimal.valueOf(100));
            } else if (discountAmount == null) {
                this.discountAmount = java.math.BigDecimal.ZERO;
            }

            BigDecimal netAmount = totalPrice.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);

            // Calculate tax
            if (taxRate != null) {
                this.taxAmount = netAmount.multiply(taxRate).divide(BigDecimal.valueOf(100));
            }

            this.netAmount = netAmount;
            this.totalAmount = netAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        }
    }

    public boolean isSubscriptionItem() {
        return itemType == InvoiceItemType.SUBSCRIPTION;
    }

    public boolean isUsageItem() {
        return itemType == InvoiceItemType.USAGE;
    }

    public boolean isDiscount() {
        return itemType == InvoiceItemType.DISCOUNT;
    }

    public boolean isTax() {
        return itemType == InvoiceItemType.TAX;
    }

    public boolean isAdjustment() {
        return itemType == InvoiceItemType.ADJUSTMENT;
    }

    public boolean isAdvancePayment() {
        return Boolean.TRUE.equals(advancePayment) || itemType == InvoiceItemType.ADVANCE_PAYMENT;
    }

    public boolean isProrated() {
        return Boolean.TRUE.equals(isProrated) || (periodStart != null && periodEnd != null &&
                daysInPeriod != null && daysBilled != null && daysBilled < daysInPeriod);
    }

    /**
     * Calculate proration factor (0.0 to 1.0)
     */
    public BigDecimal getProrationFactor() {
        if (daysInPeriod == null || daysBilled == null || daysInPeriod == 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(daysBilled).divide(BigDecimal.valueOf(daysInPeriod), 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Apply proration to this invoice item
     */
    public void applyProration(LocalDate actualStart, LocalDate actualEnd) {
        this.isProrated = true;
        this.periodStart = actualStart;
        this.periodEnd = actualEnd;

        // Calculate days
        if (periodStart != null && periodEnd != null) {
            this.daysInPeriod = (int) (periodEnd.toEpochDay() - periodStart.toEpochDay() + 1);
            this.daysBilled = (int) (actualEnd.toEpochDay() - actualStart.toEpochDay() + 1);

            // Store original price before proration
            if (this.originalUnitPrice == null) {
                this.originalUnitPrice = this.unitPrice;
            }

            // Calculate prorated price
            BigDecimal prorationFactor = getProrationFactor();
            this.unitPrice = this.originalUnitPrice.multiply(prorationFactor);

            // Recalculate amounts
            recalculateAmounts();
        }
    }

    /**
     * Mark as advance payment with specified amount
     */
    public void markAsAdvancePayment(BigDecimal advanceAmt) {
        this.advancePayment = true;
        this.advanceAmount = advanceAmt;
        this.unitPrice = advanceAmt;
        this.itemType = InvoiceItemType.ADVANCE_PAYMENT;
        recalculateAmounts();
    }

    /**
     * Calculate advance payment amount for a service
     */
    public BigDecimal calculateAdvancePayment(BigDecimal monthlyRate, int advanceMonths) {
        if (monthlyRate == null || advanceMonths <= 0) {
            return BigDecimal.ZERO;
        }
        this.advanceAmount = monthlyRate.multiply(BigDecimal.valueOf(advanceMonths));
        this.advancePayment = true;
        return this.advanceAmount;
    }

    /**
     * Get prorated amount for partial billing
     */
    public BigDecimal getProratedAmount(BigDecimal fullAmount, int daysInPeriod, int daysToBill) {
        if (fullAmount == null || daysInPeriod <= 0) {
            return BigDecimal.ZERO;
        }

        this.daysInPeriod = daysInPeriod;
        this.daysBilled = daysToBill;
        this.isProrated = true;
        this.periodStart = LocalDate.now().minusDays(daysToBill);
        this.periodEnd = LocalDate.now().minusDays(1);

        BigDecimal prorationFactor = BigDecimal.valueOf(daysToBill)
                .divide(BigDecimal.valueOf(daysInPeriod), 4, BigDecimal.ROUND_HALF_UP);

        BigDecimal proratedAmount = fullAmount.multiply(prorationFactor);
        this.unitPrice = proratedAmount;
        this.quantity = BigDecimal.ONE;

        recalculateAmounts();
        return this.totalAmount;
    }

    /**
     * Normalize advance payment to regular billing
     */
    public void normalizeAdvancePayment() {
        if (this.advancePayment && this.originalUnitPrice != null) {
            this.advancePayment = false;
            this.unitPrice = this.originalUnitPrice;
            this.isProrated = false;
            recalculateAmounts();
        }
    }

    /**
     * Check if this item is a mid-cycle change
     */
    public boolean isMidCycleChange() {
        return isProrated() && (periodStart != null && periodEnd != null &&
                (!periodStart.equals(periodStart.with(TemporalAdjusters.firstDayOfMonth())) ||
                !periodEnd.equals(periodEnd.with(TemporalAdjusters.lastDayOfMonth()))));
    }

    /**
     * Calculate remaining amount for advance payment
     */
    public BigDecimal getRemainingAdvanceAmount(BigDecimal totalServiceAmount) {
        if (!advancePayment || advanceAmount == null) {
            return BigDecimal.ZERO;
        }

        // If this is a prorated item, calculate remaining from full period
        BigDecimal fullPeriodAmount = originalUnitPrice != null ? originalUnitPrice : totalServiceAmount;
        return fullPeriodAmount.subtract(this.totalAmount);
    }

    // Getters and setters
    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public String getUsageRecordId() {
        return usageRecordId;
    }

    public void setUsageRecordId(String usageRecordId) {
        this.usageRecordId = usageRecordId;
    }

    public InvoiceItemType getItemType() {
        return itemType;
    }

    public void setItemType(InvoiceItemType itemType) {
        this.itemType = itemType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        recalculateAmounts();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalculateAmounts();
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
        recalculateAmounts();
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
        recalculateAmounts();
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
        recalculateAmounts();
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public Integer getDaysInPeriod() {
        return daysInPeriod;
    }

    public void setDaysInPeriod(Integer daysInPeriod) {
        this.daysInPeriod = daysInPeriod;
    }

    public Integer getDaysBilled() {
        return daysBilled;
    }

    public void setDaysBilled(Integer daysBilled) {
        this.daysBilled = daysBilled;
    }

    public Boolean getIsProrated() {
        return isProrated;
    }

    public void setIsProrated(Boolean isProrated) {
        this.isProrated = isProrated;
    }

    public BigDecimal getOriginalUnitPrice() {
        return originalUnitPrice;
    }

    public void setOriginalUnitPrice(BigDecimal originalUnitPrice) {
        this.originalUnitPrice = originalUnitPrice;
    }

    public Boolean getAdvancePayment() {
        return advancePayment;
    }

    public void setAdvancePayment(Boolean advancePayment) {
        this.advancePayment = advancePayment;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public void setAdvanceAmount(BigDecimal advanceAmount) {
        this.advanceAmount = advanceAmount;
    }

    public String getBillingScheme() {
        return billingScheme;
    }

    public void setBillingScheme(String billingScheme) {
        this.billingScheme = billingScheme;
    }

    /**
     * Converts JPA entity to DDD aggregate
     */
    public InvoiceItem toDomain() {
        return InvoiceItem.restore(
            this.getId(),
            null, // orderId not available in this entity
            null, // productId not available in this entity
            this.description,
            this.quantity != null ? this.quantity.intValue() : 1,
            this.unitPrice,
            this.discountAmount,
            this.taxRate,
            InvoiceItemStatus.PENDING, // Status not tracked in this entity
            this.version != null ? this.version.intValue() : 0
        );
    }

    /**
     * Creates JPA entity from DDD aggregate
     */
    public static InvoiceItemEntity from(InvoiceItem invoiceItem) {
        InvoiceItemEntity entity = new InvoiceItemEntity();
        entity.setId(invoiceItem.getId());
        entity.setDescription(invoiceItem.getDescription());
        entity.setQuantity(BigDecimal.valueOf(invoiceItem.getQuantity()));
        entity.setUnitPrice(invoiceItem.getUnitPrice());
        entity.setDiscountAmount(invoiceItem.getDiscountAmount());
        entity.setTaxRate(invoiceItem.getTaxRate());
        // Note: invoice relationship should be set by caller
        return entity;
    }
}
