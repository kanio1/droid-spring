package com.droid.bss.domain.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a resource metric
 */
public class ResourceMetric {

    private Long id;
    private Long customerId;
    private Long resourceId;
    private String metricType; // CPU, MEMORY, DISK, NETWORK, etc.
    private BigDecimal value;
    private String unit; // PERCENT, MB, GB, MBPS, etc.
    private Instant timestamp;
    private String source; // API, SNMP, AGENT, etc.

    public ResourceMetric() {
    }

    public ResourceMetric(Long customerId, Long resourceId, String metricType,
                         BigDecimal value, String unit, Instant timestamp, String source) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceMetric that = (ResourceMetric) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ResourceMetric{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", resourceId=" + resourceId +
                ", metricType='" + metricType + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                '}';
    }
}
