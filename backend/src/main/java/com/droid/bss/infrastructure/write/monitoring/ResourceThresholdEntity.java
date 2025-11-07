package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.ResourceThreshold;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * JPA entity for ResourceThreshold
 */
@Entity
@Table(name = "resource_thresholds")
public class ResourceThresholdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "warning_threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal warningThreshold;

    @Column(name = "critical_threshold", nullable = false, precision = 19, scale = 4)
    private BigDecimal criticalThreshold;

    @Column(name = "operator", nullable = false, length = 10)
    private String operator;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "consecutive_violations", nullable = false)
    private int consecutiveViolations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public ResourceThresholdEntity() {
    }

    public ResourceThresholdEntity(Long customerId, Long resourceId, String metricType,
                                   BigDecimal warningThreshold, BigDecimal criticalThreshold,
                                   String operator, String unit, boolean enabled,
                                   int consecutiveViolations) {
        this.customerId = customerId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.operator = operator;
        this.unit = unit;
        this.enabled = enabled;
        this.consecutiveViolations = consecutiveViolations;
    }

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }

    public static ResourceThresholdEntity fromDomain(ResourceThreshold threshold) {
        return new ResourceThresholdEntity(
                threshold.getCustomerId(),
                threshold.getResourceId(),
                threshold.getMetricType(),
                threshold.getWarningThreshold(),
                threshold.getCriticalThreshold(),
                threshold.getOperator(),
                threshold.getUnit(),
                threshold.isEnabled(),
                threshold.getConsecutiveViolations()
        );
    }

    public ResourceThreshold toDomain() {
        ResourceThreshold threshold = new ResourceThreshold(
                customerId,
                resourceId,
                metricType,
                warningThreshold,
                criticalThreshold,
                operator,
                unit,
                enabled,
                consecutiveViolations
        );
        threshold.setId(id);
        threshold.setCreatedAt(createdAt);
        threshold.setUpdatedAt(updatedAt);
        return threshold;
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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public BigDecimal getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(BigDecimal warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public BigDecimal getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(BigDecimal criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConsecutiveViolations() {
        return consecutiveViolations;
    }

    public void setConsecutiveViolations(int consecutiveViolations) {
        this.consecutiveViolations = consecutiveViolations;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
