package com.droid.bss.application.dto.monitoring;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for metric responses
 */
public class MetricResponse {

    private Long id;
    private Long customerId;
    private Long resourceId;
    private String metricType;
    private BigDecimal value;
    private String unit;
    private Instant timestamp;
    private String source;

    public MetricResponse() {
    }

    public MetricResponse(Long id, Long customerId, Long resourceId, String metricType,
                          BigDecimal value, String unit, Instant timestamp, String source) {
        this.id = id;
        this.customerId = customerId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
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
