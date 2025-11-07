package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity for customer resource configuration
 * Stores customer-specific resource monitoring settings
 */
public class CustomerResourceConfiguration {

    private Long id;
    private Long customerId;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String region;
    private BigDecimal maxLimit;
    private BigDecimal warningThreshold;
    private BigDecimal criticalThreshold;
    private BigDecimal budgetLimit;
    private String budgetCurrency;
    private String alertEmail;
    private String alertPhone;
    private String alertSlackWebhook;
    private boolean autoScalingEnabled;
    private BigDecimal scaleUpThreshold;
    private BigDecimal scaleDownThreshold;
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    private Instant createdAt;
    private Instant updatedAt;
    private String tags; // JSON string for flexible tagging

    public CustomerResourceConfiguration() {
    }

    public CustomerResourceConfiguration(Long customerId, String resourceType, String resourceId,
                                        String resourceName, String region, BigDecimal maxLimit,
                                        BigDecimal warningThreshold, BigDecimal criticalThreshold,
                                        BigDecimal budgetLimit, String budgetCurrency, String status) {
        this.customerId = customerId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.region = region;
        this.maxLimit = maxLimit;
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.budgetLimit = budgetLimit;
        this.budgetCurrency = budgetCurrency;
        this.status = status;
        this.autoScalingEnabled = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateTimestamps() {
        this.updatedAt = Instant.now();
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxLimit = maxLimit;
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

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public String getBudgetCurrency() {
        return budgetCurrency;
    }

    public void setBudgetCurrency(String budgetCurrency) {
        this.budgetCurrency = budgetCurrency;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }

    public String getAlertPhone() {
        return alertPhone;
    }

    public void setAlertPhone(String alertPhone) {
        this.alertPhone = alertPhone;
    }

    public String getAlertSlackWebhook() {
        return alertSlackWebhook;
    }

    public void setAlertSlackWebhook(String alertSlackWebhook) {
        this.alertSlackWebhook = alertSlackWebhook;
    }

    public boolean isAutoScalingEnabled() {
        return autoScalingEnabled;
    }

    public void setAutoScalingEnabled(boolean autoScalingEnabled) {
        this.autoScalingEnabled = autoScalingEnabled;
    }

    public BigDecimal getScaleUpThreshold() {
        return scaleUpThreshold;
    }

    public void setScaleUpThreshold(BigDecimal scaleUpThreshold) {
        this.scaleUpThreshold = scaleUpThreshold;
    }

    public BigDecimal getScaleDownThreshold() {
        return scaleDownThreshold;
    }

    public void setScaleDownThreshold(BigDecimal scaleDownThreshold) {
        this.scaleDownThreshold = scaleDownThreshold;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerResourceConfiguration that = (CustomerResourceConfiguration) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
