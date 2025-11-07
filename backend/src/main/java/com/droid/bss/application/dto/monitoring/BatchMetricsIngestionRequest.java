package com.droid.bss.application.dto.monitoring;

import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for batch metrics ingestion requests
 */
public class BatchMetricsIngestionRequest {

    @NotEmpty(message = "Metrics list cannot be empty")
    private List<MetricIngestionRequest> metrics;

    public BatchMetricsIngestionRequest() {
        this.metrics = new ArrayList<>();
    }

    public BatchMetricsIngestionRequest(List<MetricIngestionRequest> metrics) {
        this.metrics = metrics;
    }

    public List<MetricIngestionRequest> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricIngestionRequest> metrics) {
        this.metrics = metrics;
    }

    public void addMetric(MetricIngestionRequest metric) {
        if (this.metrics == null) {
            this.metrics = new ArrayList<>();
        }
        this.metrics.add(metric);
    }
}
