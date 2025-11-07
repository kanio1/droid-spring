package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity for optimization recommendations
 * Provides cost-saving suggestions based on usage analysis
 */
public class OptimizationRecommendation {

    private Long id;
    private Long customerId;
    private String resourceType;
    private String resourceId;
    private String recommendationType; // RIGHT_SIZE, SCALE_DOWN, SCALE_UP, SCHEDULE_SHUTDOWN, SWITCH_PLAN
    private String severity; // HIGH, MEDIUM, LOW
    private String title;
    private String description;
    private BigDecimal potentialSavings;
    private String currency;
    private BigDecimal currentCost;
    private BigDecimal projectedCost;
    private String status; // NEW, ACKNOWLEDGED, IMPLEMENTED, DISMISSED
    private Instant createdAt;
    private Instant acknowledgedAt;
    private Instant implementedAt;
    private String details; // JSON string with additional recommendation data

    public OptimizationRecommendation() {
    }

    public OptimizationRecommendation(Long customerId, String resourceType, String resourceId,
                                     String recommendationType, String severity, String title,
                                     String description, BigDecimal potentialSavings, String currency,
                                     BigDecimal currentCost, BigDecimal projectedCost, String status,
                                     Instant createdAt) {
        this.customerId = customerId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.recommendationType = recommendationType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.potentialSavings = potentialSavings;
        this.currency = currency;
        this.currentCost = currentCost;
        this.projectedCost = projectedCost;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void acknowledge() {
        this.status = "ACKNOWLEDGED";
        this.acknowledgedAt = Instant.now();
    }

    public void implement() {
        this.status = "IMPLEMENTED";
        this.implementedAt = Instant.now();
    }

    public void dismiss() {
        this.status = "DISMISSED";
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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPotentialSavings() {
        return potentialSavings;
    }

    public void setPotentialSavings(BigDecimal potentialSavings) {
        this.potentialSavings = potentialSavings;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getCurrentCost() {
        return currentCost;
    }

    public void setCurrentCost(BigDecimal currentCost) {
        this.currentCost = currentCost;
    }

    public BigDecimal getProjectedCost() {
        return projectedCost;
    }

    public void setProjectedCost(BigDecimal projectedCost) {
        this.projectedCost = projectedCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Instant getImplementedAt() {
        return implementedAt;
    }

    public void setImplementedAt(Instant implementedAt) {
        this.implementedAt = implementedAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimizationRecommendation that = (OptimizationRecommendation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
