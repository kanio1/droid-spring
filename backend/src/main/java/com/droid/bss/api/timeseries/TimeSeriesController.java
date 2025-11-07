package com.droid.bss.api.timeseries;

import com.droid.bss.domain.timeseries.*;
import com.droid.bss.infrastructure.timeseries.TimeSeriesQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Time-Series Metrics API
 * Provides endpoints for querying time-series data from TimescaleDB
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/timeseries")
@RequiredArgsConstructor
@Tag(name = "Time-Series Metrics", description = "Time-series data querying and analytics")
public class TimeSeriesController {

    private final TimeSeriesQueryService timeSeriesService;

    @GetMapping("/metrics/{metricName}")
    @Operation(summary = "Get time-series data for a metric", description = "Returns time-series data for the specified metric with optional time range")
    public ResponseEntity<List<TimeSeriesDataPoint>> getMetricData(
            @PathVariable String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "1h") String aggregationInterval) {

        log.debug("Fetching time-series data for metric: {}", metricName);

        if (startTime == null) {
            startTime = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        List<TimeSeriesDataPoint> data = timeSeriesService.getMetricData(
                metricName, startTime, endTime, aggregationInterval);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/metrics/{metricName}/latest")
    @Operation(summary = "Get latest metric value", description = "Returns the most recent value for a metric")
    public ResponseEntity<Double> getLatestMetricValue(@PathVariable String metricName) {
        log.debug("Fetching latest value for metric: {}", metricName);
        Double value = timeSeriesService.getLatestMetricValue(metricName);
        return ResponseEntity.ok(value);
    }

    @GetMapping("/metrics/{metricName}/statistics")
    @Operation(summary = "Get metric statistics", description = "Returns statistical summary (avg, min, max, stddev) for a metric over a time period")
    public ResponseEntity<MetricStatistics> getMetricStatistics(
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.debug("Calculating statistics for metric: {} from {} to {}", metricName, startTime, endTime);

        MetricStatistics stats = timeSeriesService.getMetricStatistics(
                metricName, startTime, endTime);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/metrics/{metricName}/trend")
    @Operation(summary = "Get metric trend", description = "Returns trend analysis comparing current period with previous period")
    public ResponseEntity<MetricTrend> getMetricTrend(
            @PathVariable String metricName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant currentPeriodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant currentPeriodEnd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant previousPeriodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant previousPeriodEnd) {

        log.debug("Calculating trend for metric: {}", metricName);

        MetricTrend trend = timeSeriesService.getMetricTrend(
                metricName, currentPeriodStart, currentPeriodEnd,
                previousPeriodStart, previousPeriodEnd);

        return ResponseEntity.ok(trend);
    }

    @GetMapping("/metrics/top")
    @Operation(summary = "Get top metrics", description = "Returns top N metrics by maximum value in the specified time period")
    public ResponseEntity<List<MetricSummary>> getTopMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("Getting top {} metrics from {} to {}", limit, startTime, endTime);

        List<MetricSummary> metrics = timeSeriesService.getTopMetrics(
                startTime, endTime, limit);

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/business-metrics/{metricName}")
    @Operation(summary = "Get business metric data", description = "Returns time-series data for a business metric")
    public ResponseEntity<List<BusinessMetricDataPoint>> getBusinessMetricData(
            @PathVariable String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.debug("Fetching business metric data: {}", metricName);

        if (startTime == null) {
            startTime = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        List<BusinessMetricDataPoint> data = timeSeriesService.getBusinessMetricData(
                metricName, startTime, endTime);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/resource-metrics")
    @Operation(summary = "Get resource metrics", description = "Returns time-series data for system resource metrics (CPU, memory, disk)")
    public ResponseEntity<List<ResourceMetricDataPoint>> getResourceMetricData(
            @RequestParam String host,
            @RequestParam String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.debug("Fetching resource metrics: {} - {}", host, resourceType);

        if (startTime == null) {
            startTime = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        List<ResourceMetricDataPoint> data = timeSeriesService.getResourceMetricData(
                host, resourceType, startTime, endTime);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/capacity-planning/{resourceType}")
    @Operation(summary = "Get capacity planning data", description = "Returns capacity planning analysis for a resource type")
    public ResponseEntity<CapacityPlanningData> getCapacityPlanningData(
            @PathVariable String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        log.debug("Getting capacity planning data for: {}", resourceType);

        if (startTime == null) {
            startTime = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        CapacityPlanningData data = timeSeriesService.getCapacityPlanningData(
                resourceType, startTime, endTime);

        return ResponseEntity.ok(data);
    }

    @PostMapping("/metrics/{metricName}")
    @Operation(summary = "Insert performance metric", description = "Inserts a new performance metric data point")
    public ResponseEntity<Map<String, String>> insertPerformanceMetric(
            @PathVariable String metricName,
            @RequestBody InsertMetricRequest request) {

        log.debug("Inserting performance metric: {}", metricName);

        timeSeriesService.insertPerformanceMetric(
                metricName,
                request.getValue(),
                request.getUnit(),
                request.getTags());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Metric inserted successfully",
                "metricName", metricName
        ));
    }

    @PostMapping("/business-metrics/{metricName}")
    @Operation(summary = "Insert business metric", description = "Inserts a new business metric data point")
    public ResponseEntity<Map<String, String>> insertBusinessMetric(
            @PathVariable String metricName,
            @RequestBody InsertBusinessMetricRequest request) {

        log.debug("Inserting business metric: {}", metricName);

        timeSeriesService.insertBusinessMetric(
                metricName,
                request.getValue(),
                request.getUnit(),
                request.getCustomerId(),
                request.getProductId(),
                request.getMetadata());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Business metric inserted successfully",
                "metricName", metricName
        ));
    }

    /**
     * Request DTO for inserting a performance metric
     */
    public static class InsertMetricRequest {
        private Double value;
        private String unit;
        private Map<String, Object> tags;

        // Getters and setters
        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Map<String, Object> getTags() {
            return tags;
        }

        public void setTags(Map<String, Object> tags) {
            this.tags = tags;
        }
    }

    /**
     * Request DTO for inserting a business metric
     */
    public static class InsertBusinessMetricRequest {
        private Double value;
        private String unit;
        private UUID customerId;
        private UUID productId;
        private Map<String, Object> metadata;

        // Getters and setters
        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public UUID getCustomerId() {
            return customerId;
        }

        public void setCustomerId(UUID customerId) {
            this.customerId = customerId;
        }

        public UUID getProductId() {
            return productId;
        }

        public void setProductId(UUID productId) {
            this.productId = productId;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
