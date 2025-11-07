package com.droid.bss.infrastructure.database.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer_resource_configurations")
public class CustomerResourceConfigurationEntity {

    @Id
    @GeneratedValue(generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "region")
    private String region;

    @Column(name = "max_limit", precision = 19, scale = 2)
    private BigDecimal maxLimit;

    @Column(name = "warning_threshold", precision = 5, scale = 2)
    private BigDecimal warningThreshold;

    @Column(name = "critical_threshold", precision = 5, scale = 2)
    private BigDecimal criticalThreshold;

    @Column(name = "budget_limit", precision = 19, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "budget_currency")
    private String budgetCurrency;

    @Column(name = "alert_email")
    private String alertEmail;

    @Column(name = "alert_phone")
    private String alertPhone;

    @Column(name = "alert_slack_webhook")
    private String alertSlackWebhook;

    @Column(name = "auto_scaling_enabled")
    private boolean autoScalingEnabled;

    @Column(name = "scale_up_threshold", precision = 5, scale = 2)
    private BigDecimal scaleUpThreshold;

    @Column(name = "scale_down_threshold", precision = 5, scale = 2)
    private BigDecimal scaleDownThreshold;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
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
}
