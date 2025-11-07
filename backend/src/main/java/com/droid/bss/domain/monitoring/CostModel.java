package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain entity for cost models
 * Defines billing models and rates for resource usage
 */
public class CostModel {

    private Long id;
    private String modelName;
    private String description;
    private String billingPeriod; // hourly, daily, monthly, yearly
    private BigDecimal baseCost;
    private BigDecimal overageRate;
    private BigDecimal includedUsage;
    private String currency;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public CostModel() {
    }

    public CostModel(String modelName, String description, String billingPeriod,
                     BigDecimal baseCost, BigDecimal overageRate, BigDecimal includedUsage,
                     String currency) {
        this.modelName = modelName;
        this.description = description;
        this.billingPeriod = billingPeriod;
        this.baseCost = baseCost;
        this.overageRate = overageRate;
        this.includedUsage = includedUsage;
        this.currency = currency;
        this.active = true;
    }

    /**
     * Calculate cost for given usage
     */
    public BigDecimal calculateCost(BigDecimal usage) {
        if (usage.compareTo(includedUsage) <= 0) {
            return baseCost;
        }

        BigDecimal overage = usage.subtract(includedUsage);
        return baseCost.add(overage.multiply(overageRate));
    }

    /**
     * Calculate overage cost only
     */
    public BigDecimal calculateOverageCost(BigDecimal usage) {
        if (usage.compareTo(includedUsage) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal overage = usage.subtract(includedUsage);
        return overage.multiply(overageRate);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getOverageRate() {
        return overageRate;
    }

    public void setOverageRate(BigDecimal overageRate) {
        this.overageRate = overageRate;
    }

    public BigDecimal getIncludedUsage() {
        return includedUsage;
    }

    public void setIncludedUsage(BigDecimal includedUsage) {
        this.includedUsage = includedUsage;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
