package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.Alert;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for Alert
 */
@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "current_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "threshold_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    @Column(name = "threshold_type", nullable = false, length = 20)
    private String thresholdType;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "source", length = 50)
    private String source;

    public AlertEntity() {
    }

    public AlertEntity(Long customerId, Long resourceId, String metricType, String severity,
                       String status, BigDecimal currentValue, BigDecimal thresholdValue,
                       String thresholdType, String message, Instant triggeredAt, String source) {
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
        this.source = source;
    }

    public static AlertEntity fromDomain(Alert alert) {
        return new AlertEntity(
                alert.getCustomerId(),
                alert.getResourceId(),
                alert.getMetricType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getCurrentValue(),
                alert.getThresholdValue(),
                alert.getThresholdType(),
                alert.getMessage(),
                alert.getTriggeredAt(),
                alert.getSource()
        );
    }

    public Alert toDomain() {
        Alert alert = new Alert(
                customerId,
                resourceId,
                metricType,
                severity,
                status,
                currentValue,
                thresholdValue,
                thresholdType,
                message,
                triggeredAt,
                source
        );
        alert.setId(id);
        alert.setResolvedAt(resolvedAt);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(acknowledgedAt);
        alert.setResolvedBy(resolvedBy);
        return alert;
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
