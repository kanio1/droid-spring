package com.droid.bss.domain.fraud;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Fraud detection rule entity
 */
@Entity
@Table(name = "fraud_rules")
@EntityListeners(AuditingEntityListener.class)
public class FraudRuleEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String ruleCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudRuleType ruleType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column
    private Integer priority = 5;

    @Column
    private String conditions;

    @Column
    private String action;

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal riskScoreIncrease;

    @Column
    private String alertTemplate;

    @Column
    private String category;

    @Column
    private String sourceEntity;

    @Column
    private Boolean requiresReview = false;

    @Column
    private Integer maxAlertsPerDay;

    @Column
    private Integer alertCountToday = 0;

    @Column
    private LocalDateTime lastTriggeredAt;

    @Column
    private String createdBy;

    @Column
    private String updatedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public FraudRuleEntity() {
    }

    public FraudRuleEntity(String ruleCode, String name, FraudRuleType ruleType) {
        this.ruleCode = ruleCode;
        this.name = name;
        this.ruleType = ruleType;
    }

    // Business methods
    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void trigger() {
        if (enabled) {
            this.lastTriggeredAt = LocalDateTime.now();
            incrementAlertCount();
        }
    }

    public void incrementAlertCount() {
        this.alertCountToday = (this.alertCountToday != null ? this.alertCountToday : 0) + 1;
    }

    public void resetDailyCount() {
        this.alertCountToday = 0;
    }

    public boolean hasReachedDailyLimit() {
        if (maxAlertsPerDay == null) return false;
        return alertCountToday >= maxAlertsPerDay;
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getRuleCode() { return ruleCode; }

    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public FraudRuleType getRuleType() { return ruleType; }

    public void setRuleType(FraudRuleType ruleType) { this.ruleType = ruleType; }

    public Boolean getEnabled() { return enabled; }

    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Integer getPriority() { return priority; }

    public void setPriority(Integer priority) { this.priority = priority; }

    public String getConditions() { return conditions; }

    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getAction() { return action; }

    public void setAction(String action) { this.action = action; }

    public java.math.BigDecimal getRiskScoreIncrease() { return riskScoreIncrease; }

    public void setRiskScoreIncrease(java.math.BigDecimal riskScoreIncrease) { this.riskScoreIncrease = riskScoreIncrease; }

    public String getAlertTemplate() { return alertTemplate; }

    public void setAlertTemplate(String alertTemplate) { this.alertTemplate = alertTemplate; }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }

    public String getSourceEntity() { return sourceEntity; }

    public void setSourceEntity(String sourceEntity) { this.sourceEntity = sourceEntity; }

    public Boolean getRequiresReview() { return requiresReview; }

    public void setRequiresReview(Boolean requiresReview) { this.requiresReview = requiresReview; }

    public Integer getMaxAlertsPerDay() { return maxAlertsPerDay; }

    public void setMaxAlertsPerDay(Integer maxAlertsPerDay) { this.maxAlertsPerDay = maxAlertsPerDay; }

    public Integer getAlertCountToday() { return alertCountToday; }

    public void setAlertCountToday(Integer alertCountToday) { this.alertCountToday = alertCountToday; }

    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }

    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }

    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }

    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
