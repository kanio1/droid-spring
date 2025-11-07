package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain entity for cost calculations
 * Tracks calculated costs for resources over specific periods
 */
public class CostCalculation {

    private Long id;
    private Long customerId;
    private String resourceType;
    private String billingPeriod; // hourly, daily, monthly
    private Instant periodStart;
    private Instant periodEnd;
    private BigDecimal totalUsage;
    private BigDecimal baseCost;
    private BigDecimal overageCost;
    private BigDecimal totalCost;
    private String currency;
    private CostModel costModel;
    private String status; // DRAFT, FINAL, INVOICED
    private Instant calculatedAt;

    public CostCalculation() {
    }

    public CostCalculation(Long customerId, String resourceType, String billingPeriod,
                          Instant periodStart, Instant periodEnd, BigDecimal totalUsage,
                          CostModel costModel, String currency) {
        this.customerId = customerId;
        this.resourceType = resourceType;
        this.billingPeriod = billingPeriod;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalUsage = totalUsage;
        this.costModel = costModel;
        this.currency = currency;
        this.status = "DRAFT";
        this.calculatedAt = Instant.now();
        calculateCosts();
    }

    /**
     * Calculate costs based on usage and cost model
     */
    public void calculateCosts() {
        if (costModel != null) {
            this.baseCost = costModel.getBaseCost();
            this.overageCost = costModel.calculateOverageCost(totalUsage);
            this.totalCost = costModel.calculateCost(totalUsage);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Instant periodStart) {
        this.periodStart = periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Instant periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(BigDecimal totalUsage) {
        this.totalUsage = totalUsage;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getOverageCost() {
        return overageCost;
    }

    public void setOverageCost(BigDecimal overageCost) {
        this.overageCost = overageCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public CostModel getCostModel() {
        return costModel;
    }

    public void setCostModel(CostModel costModel) {
        this.costModel = costModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
