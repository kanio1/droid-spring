package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity for cost forecasts
 * Stores predicted costs based on historical trends
 */
public class CostForecast {

    private Long id;
    private Long customerId;
    private String resourceType;
    private String billingPeriod;
    private Instant forecastPeriodStart;
    private Instant forecastPeriodEnd;
    private BigDecimal predictedCost;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;
    private String trendDirection; // INCREASING, DECREASING, STABLE
    private Double confidenceLevel;
    private Instant calculatedAt;
    private String forecastModel; // LINEAR_REGRESSION, MOVING_AVERAGE

    public CostForecast() {
    }

    public CostForecast(Long customerId, String resourceType, String billingPeriod,
                       Instant forecastPeriodStart, Instant forecastPeriodEnd,
                       BigDecimal predictedCost, BigDecimal lowerBound, BigDecimal upperBound,
                       String trendDirection, Double confidenceLevel, String forecastModel) {
        this.customerId = customerId;
        this.resourceType = resourceType;
        this.billingPeriod = billingPeriod;
        this.forecastPeriodStart = forecastPeriodStart;
        this.forecastPeriodEnd = forecastPeriodEnd;
        this.predictedCost = predictedCost;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.trendDirection = trendDirection;
        this.confidenceLevel = confidenceLevel;
        this.forecastModel = forecastModel;
        this.calculatedAt = Instant.now();
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
        CostForecast that = (CostForecast) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
