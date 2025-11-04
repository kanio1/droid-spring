package com.droid.bss.domain.invoice;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
