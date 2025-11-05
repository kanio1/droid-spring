package com.droid.bss.domain.billing;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.product.ProductEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Rating rules for usage charges
 */
@Entity
@Table(name = "rating_rules")
public class RatingRuleEntity extends BaseEntity {

    @NotNull
    @Size(max = 100)
    @Column(unique = true, nullable = false)
    private String ruleCode;

    @NotNull
    @Size(max = 200)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type")
    private DestinationType destinationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_period")
    private RatePeriod ratePeriod;

    @NotNull
    @Column(name = "unit_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal unitRate;

    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PLN";

    @NotNull
    @Column(name = "minimum_units", nullable = false)
    private Long minimumUnits = 1L;

    @Column(name = "maximum_units")
    private Long maximumUnits;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    protected RatingRuleEntity() {
    }

    public RatingRuleEntity(
            String ruleCode,
            String name,
            UsageType usageType,
            BigDecimal unitRate,
            LocalDate effectiveFrom) {
        this.ruleCode = ruleCode;
        this.name = name;
        this.usageType = usageType;
        this.unitRate = unitRate;
        this.effectiveFrom = effectiveFrom;
    }

    // Getters and Setters
    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getMinimumUnits() {
        return minimumUnits;
    }

    public void setMinimumUnits(Long minimumUnits) {
        this.minimumUnits = minimumUnits;
    }

    public Long getMaximumUnits() {
        return maximumUnits;
    }

    public void setMaximumUnits(Long maximumUnits) {
        this.maximumUnits = maximumUnits;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public ProductEntity getProduct() {
        return product;
    }

    public void setProduct(ProductEntity product) {
        this.product = product;
    }

    // Business methods
    public boolean isActive() {
        return Boolean.TRUE.equals(this.active);
    }

    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) return false;
        if (date.isBefore(this.effectiveFrom)) return false;
        if (this.effectiveTo != null && date.isAfter(this.effectiveTo)) return false;
        return true;
    }

    public boolean matches(UsageType type, DestinationType destination, RatePeriod period) {
        if (this.usageType != type) return false;
        if (this.destinationType != null && this.destinationType != destination) return false;
        if (this.ratePeriod != null && this.ratePeriod != period) return false;
        return true;
    }
}
