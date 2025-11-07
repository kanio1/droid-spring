package com.droid.bss.infrastructure.database.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cost_forecasts")
public class CostForecastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "billing_period", nullable = false)
    private String billingPeriod;

    @Column(name = "forecast_period_start", nullable = false)
    private Instant forecastPeriodStart;

    @Column(name = "forecast_period_end", nullable = false)
    private Instant forecastPeriodEnd;

    @Column(name = "predicted_cost", nullable = false)
    private BigDecimal predictedCost;

    @Column(name = "lower_bound", nullable = false)
    private BigDecimal lowerBound;

    @Column(name = "upper_bound", nullable = false)
    private BigDecimal upperBound;

    @Column(name = "trend_direction", nullable = false)
    private String trendDirection;

    @Column(name = "confidence_level", nullable = false)
    private Double confidenceLevel;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "forecast_model", nullable = false)
    private String forecastModel;

    public CostForecastEntity() {
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

    public Instant getForecastPeriodStart() {
        return forecastPeriodStart;
    }

    public void setForecastPeriodStart(Instant forecastPeriodStart) {
        this.forecastPeriodStart = forecastPeriodStart;
    }

    public Instant getForecastPeriodEnd() {
        return forecastPeriodEnd;
    }

    public void setForecastPeriodEnd(Instant forecastPeriodEnd) {
        this.forecastPeriodEnd = forecastPeriodEnd;
    }

    public BigDecimal getPredictedCost() {
        return predictedCost;
    }

    public void setPredictedCost(BigDecimal predictedCost) {
        this.predictedCost = predictedCost;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getForecastModel() {
        return forecastModel;
    }

    public void setForecastModel(String forecastModel) {
        this.forecastModel = forecastModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CostForecastEntity that = (CostForecastEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
