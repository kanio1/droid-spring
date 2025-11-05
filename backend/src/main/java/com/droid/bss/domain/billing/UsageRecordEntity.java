package com.droid.bss.domain.billing;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entity representing service usage (CDR - Call Detail Records)
 */
@Entity
@Table(name = "usage_records")
public class UsageRecordEntity extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private SubscriptionEntity subscription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_unit", nullable = false)
    private UsageUnit usageUnit;

    @NotNull
    @Column(name = "usage_amount", nullable = false, precision = 15, scale = 3)
    private BigDecimal usageAmount;

    @NotNull
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @NotNull
    @Column(name = "usage_time", nullable = false)
    private LocalTime usageTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type")
    private DestinationType destinationType;

    @Column(name = "destination_number", length = 50)
    private String destinationNumber;

    @Column(name = "destination_country", length = 2)
    private String destinationCountry;

    @Column(name = "network_id", length = 50)
    private String networkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_period")
    private RatePeriod ratePeriod;

    @Column(name = "unit_rate", precision = 8, scale = 4)
    private BigDecimal unitRate;

    @Column(name = "charge_amount", precision = 10, scale = 2)
    private BigDecimal chargeAmount;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("23.00");

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "rated")
    private Boolean rated = false;

    @Column(name = "rating_date")
    private LocalDate ratingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private UsageSource source;

    @Column(name = "source_file", length = 200)
    private String sourceFile;

    @Column(name = "processed")
    private Boolean processed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;

    protected UsageRecordEntity() {
    }

    public UsageRecordEntity(
            SubscriptionEntity subscription,
            UsageType usageType,
            UsageUnit usageUnit,
            BigDecimal usageAmount,
            LocalDate usageDate,
            LocalTime usageTime) {
        this.subscription = subscription;
        this.usageType = usageType;
        this.usageUnit = usageUnit;
        this.usageAmount = usageAmount;
        this.usageDate = usageDate;
        this.usageTime = usageTime;
    }

    // Getters and Setters
    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public UsageUnit getUsageUnit() {
        return usageUnit;
    }

    public void setUsageUnit(UsageUnit usageUnit) {
        this.usageUnit = usageUnit;
    }

    public BigDecimal getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(BigDecimal usageAmount) {
        this.usageAmount = usageAmount;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public LocalTime getUsageTime() {
        return usageTime;
    }

    public void setUsageTime(LocalTime usageTime) {
        this.usageTime = usageTime;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public String getDestinationNumber() {
        return destinationNumber;
    }

    public void setDestinationNumber(String destinationNumber) {
        this.destinationNumber = destinationNumber;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public RatePeriod getRatePeriod() {
        return ratePeriod;
    }

    public void setRatePeriod(RatePeriod ratePeriod) {
        this.ratePeriod = ratePeriod;
    }

    public BigDecimal getUnitRate() {
        return unitRate;
    }

    public void setUnitRate(BigDecimal unitRate) {
        this.unitRate = unitRate;
    }

    public BigDecimal getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(BigDecimal chargeAmount) {
        this.chargeAmount = chargeAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Boolean getRated() {
        return rated;
    }

    public void setRated(Boolean rated) {
        this.rated = rated;
    }

    public LocalDate getRatingDate() {
        return ratingDate;
    }

    public void setRatingDate(LocalDate ratingDate) {
        this.ratingDate = ratingDate;
    }

    public UsageSource getSource() {
        return source;
    }

    public void setSource(UsageSource source) {
        this.source = source;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    // Business methods
    public boolean isRated() {
        return Boolean.TRUE.equals(this.rated);
    }

    public boolean isProcessed() {
        return Boolean.TRUE.equals(this.processed);
    }

    public void calculateTotals() {
        if (this.chargeAmount != null && this.taxRate != null) {
            this.taxAmount = this.chargeAmount.multiply(this.taxRate).divide(new BigDecimal("100"));
            this.totalAmount = this.chargeAmount.add(this.taxAmount != null ? this.taxAmount : BigDecimal.ZERO);
        }
    }
}
