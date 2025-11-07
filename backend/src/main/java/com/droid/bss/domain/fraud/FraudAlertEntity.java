package com.droid.bss.domain.fraud;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Fraud alert entity for tracking suspicious activities
 */
@Entity
@Table(name = "fraud_alerts")
@EntityListeners(AuditingEntityListener.class)
public class FraudAlertEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String alertId;

    @Column(nullable = false)
    private String customerId;

    @Column
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudAlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudAlertType alertType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String severity;

    @Column
    private String ruleTriggered;

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal riskScore;

    @Column
    private String sourceEntityType;

    @Column
    private String sourceEntityId;

    @Column
    private String transactionId;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String location;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column
    private String assignedTo;

    @Column
    private LocalDateTime assignedAt;

    @Column
    private String resolvedBy;

    @Column
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column
    private Boolean falsePositive = false;

    @Column
    private String escalatedTo;

    @Column
    private LocalDateTime escalatedAt;

    @Column
    private Integer reviewCount = 0;

    @Column
    private String sourceSystem;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public FraudAlertEntity() {
    }

    public FraudAlertEntity(String alertId, String customerId, FraudAlertType alertType, String title, String description) {
        this.alertId = alertId;
        this.customerId = customerId;
        this.alertType = alertType;
        this.title = title;
        this.description = description;
        this.status = FraudAlertStatus.NEW;
        this.severity = "MEDIUM";
        this.riskScore = java.math.BigDecimal.ZERO;
    }

    // Business methods
    public void assignTo(String analystId) {
        this.assignedTo = analystId;
        this.assignedAt = LocalDateTime.now();
        if (this.status == FraudAlertStatus.NEW) {
            this.status = FraudAlertStatus.ASSIGNED;
        }
    }

    public void escalate(String escalatedToLevel) {
        this.escalatedTo = escalatedToLevel;
        this.escalatedAt = LocalDateTime.now();
        this.status = FraudAlertStatus.ESCALATED;
    }

    public void resolve(String resolvedBy, String resolutionNotes) {
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = LocalDateTime.now();
        this.status = FraudAlertStatus.RESOLVED;
    }

    public void markAsFalsePositive(String notedBy, String reason) {
        this.falsePositive = true;
        this.status = FraudAlertStatus.CLOSED;
        this.resolutionNotes = "FALSE POSITIVE: " + reason;
        this.resolvedBy = notedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public void reject(String rejectedBy, String reason) {
        this.status = FraudAlertStatus.REJECTED;
        this.resolutionNotes = "REJECTED: " + reason;
        this.resolvedBy = rejectedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public void updateRiskScore(java.math.BigDecimal newScore) {
        this.riskScore = newScore;
        updateSeverity();
    }

    private void updateSeverity() {
        if (riskScore.compareTo(new java.math.BigDecimal("80")) >= 0) {
            this.severity = "CRITICAL";
        } else if (riskScore.compareTo(new java.math.BigDecimal("60")) >= 0) {
            this.severity = "HIGH";
        } else if (riskScore.compareTo(new java.math.BigDecimal("30")) >= 0) {
            this.severity = "MEDIUM";
        } else {
            this.severity = "LOW";
        }
    }

    public boolean isHighRisk() {
        return riskScore.compareTo(new java.math.BigDecimal("60")) >= 0;
    }

    public boolean isOpen() {
        return status == FraudAlertStatus.NEW || status == FraudAlertStatus.ASSIGNED || status == FraudAlertStatus.ESCALATED;
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getAlertId() { return alertId; }

    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getCustomerId() { return customerId; }

    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getAccountId() { return accountId; }

    public void setAccountId(String accountId) { this.accountId = accountId; }

    public FraudAlertStatus getStatus() { return status; }

    public void setStatus(FraudAlertStatus status) { this.status = status; }

    public FraudAlertType getAlertType() { return alertType; }

    public void setAlertType(FraudAlertType alertType) { this.alertType = alertType; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }

    public void setSeverity(String severity) { this.severity = severity; }

    public String getRuleTriggered() { return ruleTriggered; }

    public void setRuleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; }

    public java.math.BigDecimal getRiskScore() { return riskScore; }

    public void setRiskScore(java.math.BigDecimal riskScore) { this.riskScore = riskScore; }

    public String getSourceEntityType() { return sourceEntityType; }

    public void setSourceEntityType(String sourceEntityType) { this.sourceEntityType = sourceEntityType; }

    public String getSourceEntityId() { return sourceEntityId; }

    public void setSourceEntityId(String sourceEntityId) { this.sourceEntityId = sourceEntityId; }

    public String getTransactionId() { return transactionId; }

    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getIpAddress() { return ipAddress; }

    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }

    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public String getDetails() { return details; }

    public void setDetails(String details) { this.details = details; }

    public String getAssignedTo() { return assignedTo; }

    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getAssignedAt() { return assignedAt; }

    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public String getResolvedBy() { return resolvedBy; }

    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }

    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNotes() { return resolutionNotes; }

    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Boolean getFalsePositive() { return falsePositive; }

    public void setFalsePositive(Boolean falsePositive) { this.falsePositive = falsePositive; }

    public String getEscalatedTo() { return escalatedTo; }

    public void setEscalatedTo(String escalatedTo) { this.escalatedTo = escalatedTo; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }

    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public Integer getReviewCount() { return reviewCount; }

    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getSourceSystem() { return sourceSystem; }

    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
