package com.droid.bss.application.dto.monitoring;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for metric ingestion requests
 */
public class MetricIngestionRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotBlank(message = "Metric type is required")
    @Size(max = 50, message = "Metric type must not exceed 50 characters")
    private String metricType; // CPU, MEMORY, DISK, NETWORK

    @NotNull(message = "Value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Value must be greater than 0")
    private BigDecimal value;

    @NotBlank(message = "Unit is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit; // PERCENT, MB, GB, MBPS, etc.

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;

    @Size(max = 50, message = "Source must not exceed 50 characters")
    private String source; // API, SNMP, AGENT

    public MetricIngestionRequest() {
    }

    public MetricIngestionRequest(Long customerId, Long resourceId, String metricType,
                                  BigDecimal value, String unit, Instant timestamp, String source) {
        this.customerId = customerId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
        this.source = source;
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

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
