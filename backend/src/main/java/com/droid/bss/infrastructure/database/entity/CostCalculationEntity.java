package com.droid.bss.infrastructure.database.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for CostCalculation
 */
@Entity
@Table(name = "cost_calculations")
public class CostCalculationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "total_usage", nullable = false)
    private BigDecimal totalUsage;

    @Column(name = "base_cost", nullable = false)
    private BigDecimal baseCost;

    @Column(name = "overage_cost", nullable = false)
    private BigDecimal overageCost;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @Column(name = "currency", nullable = false)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_model_id")
    private CostModelEntity costModel;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    public CostCalculationEntity() {
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

    public CostModelEntity getCostModel() {
        return costModel;
    }

    public void setCostModel(CostModelEntity costModel) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CostCalculationEntity that = (CostCalculationEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
