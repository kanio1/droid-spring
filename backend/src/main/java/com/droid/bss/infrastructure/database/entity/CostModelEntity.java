package com.droid.bss.infrastructure.database.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for CostModel
 */
@Entity
@Table(name = "cost_models")
public class CostModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, unique = true)
    private String modelName;

    @Column(name = "description")
    private String description;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Column(name = "base_cost", nullable = false)
    private BigDecimal baseCost;

    @Column(name = "overage_rate", nullable = false)
    private BigDecimal overageRate;

    @Column(name = "included_usage", nullable = false)
    private BigDecimal includedUsage;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public CostModelEntity() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CostModelEntity that = (CostModelEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
