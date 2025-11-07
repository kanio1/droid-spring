package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain entity representing a resource threshold for alerting
 */
public class ResourceThreshold {

    private Long id;
    private Long customerId;
    private Long resourceId;
    private String metricType; // CPU, MEMORY, DISK, NETWORK
    private BigDecimal warningThreshold;
    private BigDecimal criticalThreshold;
    private String operator; // GT (greater than), LT (less than)
    private String unit; // PERCENT, MB, GB, MBPS, etc.
    private boolean enabled;
    private int consecutiveViolations; // Number of consecutive violations before alert
    private Long createdAt;
    private Long updatedAt;

    public ResourceThreshold() {
    }

    public ResourceThreshold(Long customerId, Long resourceId, String metricType,
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

    public boolean isThresholdViolated(BigDecimal value) {
        if (!enabled) {
            return false;
        }

        if ("GT".equals(operator)) {
            return value.compareTo(criticalThreshold) > 0 || value.compareTo(warningThreshold) > 0;
        } else if ("LT".equals(operator)) {
            return value.compareTo(criticalThreshold) < 0 || value.compareTo(warningThreshold) < 0;
        }
        return false;
    }

    public String getSeverity(BigDecimal value) {
        if (!enabled) {
            return "OK";
        }

        if ("GT".equals(operator)) {
            if (value.compareTo(criticalThreshold) > 0) {
                return "CRITICAL";
            } else if (value.compareTo(warningThreshold) > 0) {
                return "WARNING";
            }
        } else if ("LT".equals(operator)) {
            if (value.compareTo(criticalThreshold) < 0) {
                return "CRITICAL";
            } else if (value.compareTo(warningThreshold) < 0) {
                return "WARNING";
            }
        }
        return "OK";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceThreshold that = (ResourceThreshold) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ResourceThreshold{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", resourceId=" + resourceId +
                ", metricType='" + metricType + '\'' +
                ", warningThreshold=" + warningThreshold +
                ", criticalThreshold=" + criticalThreshold +
                ", operator='" + operator + '\'' +
                ", unit='" + unit + '\'' +
                ", enabled=" + enabled +
                ", consecutiveViolations=" + consecutiveViolations +
                '}';
    }
}
