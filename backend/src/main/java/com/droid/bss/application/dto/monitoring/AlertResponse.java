package com.droid.bss.application.dto.monitoring;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for alert responses
 */
public class AlertResponse {

    private Long id;
    private Long customerId;
    private Long resourceId;
    private String metricType;
    private String severity;
    private String status;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private String thresholdType;
    private String message;
    private Instant triggeredAt;
    private Instant resolvedAt;
    private String acknowledgedBy;
    private Instant acknowledgedAt;
    private String resolvedBy;
    private String source;

    public AlertResponse() {
    }

    public AlertResponse(Long id, Long customerId, Long resourceId, String metricType,
                        String severity, String status, BigDecimal currentValue,
                        BigDecimal thresholdValue, String thresholdType, String message,
                        Instant triggeredAt, Instant resolvedAt, String acknowledgedBy,
                        Instant acknowledgedAt, String resolvedBy, String source) {
        this.id = id;
        this.customerId = customerId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.severity = severity;
        this.status = status;
        this.currentValue = currentValue;
        this.thresholdValue = thresholdValue;
        this.thresholdType = thresholdType;
        this.message = message;
        this.triggeredAt = triggeredAt;
        this.resolvedAt = resolvedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedBy = resolvedBy;
        this.source = source;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getThresholdType() {
        return thresholdType;
    }

    public void setThresholdType(String thresholdType) {
        this.thresholdType = thresholdType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
