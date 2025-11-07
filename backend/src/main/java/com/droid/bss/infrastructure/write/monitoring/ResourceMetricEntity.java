package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.ResourceMetric;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for ResourceMetric
 */
@Entity
@Table(name = "usage_metrics")
public class ResourceMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "value", nullable = false, precision = 19, scale = 4)
    private BigDecimal value;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "source", length = 50)
    private String source;

    public ResourceMetricEntity() {
    }

    public ResourceMetricEntity(Long customerId, Long resourceId, String metricType,
                               BigDecimal value, String unit, Instant timestamp, String source) {
        this.customerId = customerId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
        this.source = source;
    }

    public static ResourceMetricEntity fromDomain(ResourceMetric metric) {
        return new ResourceMetricEntity(
                metric.getCustomerId(),
                metric.getResourceId(),
                metric.getMetricType(),
                metric.getValue(),
                metric.getUnit(),
                metric.getTimestamp(),
                metric.getSource()
        );
    }

    public ResourceMetric toDomain() {
        ResourceMetric metric = new ResourceMetric(
                customerId,
                resourceId,
                metricType,
                value,
                unit,
                timestamp,
                source
        );
        metric.setId(id);
        return metric;
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
