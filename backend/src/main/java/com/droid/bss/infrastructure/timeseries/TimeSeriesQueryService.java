package com.droid.bss.infrastructure.timeseries;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Time-Series Query Service
 * Provides methods for querying time-series data from TimescaleDB hypertables
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSeriesQueryService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get time-series data for a specific metric
     */
    public List<TimeSeriesDataPoint> getMetricData(String metricName,
                                                   Instant startTime,
                                                   Instant endTime,
                                                   String aggregationInterval) {
        log.debug("Fetching metric data: {} from {} to {} with interval: {}",
                metricName, startTime, endTime, aggregationInterval);

        String query = """
            SELECT bucket as time, avg_value, min_value, max_value, sample_count
            FROM performance_metrics_1h
            WHERE metric_name = ?
            AND bucket >= ?
            AND bucket <= ?
            ORDER BY bucket ASC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, metricName, startTime, endTime);

        return rows.stream()
                .map(this::mapToTimeSeriesDataPoint)
                .collect(Collectors.toList());
    }

    /**
     * Get current metric value (latest)
     */
    public Double getLatestMetricValue(String metricName) {
        String query = """
            SELECT metric_value
            FROM performance_metrics
            WHERE metric_name = ?
            ORDER BY time DESC
            LIMIT 1
 """;

        List<Double> results = jdbcTemplate.queryForList(query, Double.class, metricName);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get metric statistics over a time period
     */
    public MetricStatistics getMetricStatistics(String metricName,
                                                Instant startTime,
                                                Instant endTime) {
        log.debug("Calculating statistics for metric: {} from {} to {}",
                metricName, startTime, endTime);

        String query = """
            SELECT
                AVG(metric_value) as avg_value,
                MIN(metric_value) as min_value,
                MAX(metric_value) as max_value,
                STDDEV(metric_value) as stddev_value,
                COUNT(*) as sample_count
            FROM performance_metrics
            WHERE metric_name = ?
            AND time >= ?
            AND time <= ?
 """;

        Map<String, Object> row = jdbcTemplate.queryForMap(query, metricName, startTime, endTime);

        return MetricStatistics.builder()
                .metricName(metricName)
                .average(getDouble(row, "avg_value"))
                .minimum(getDouble(row, "min_value"))
                .maximum(getDouble(row, "max_value"))
                .standardDeviation(getDouble(row, "stddev_value"))
                .sampleCount(getLong(row, "sample_count"))
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    /**
     * Get trend data for a metric (percentage change)
     */
    public MetricTrend getMetricTrend(String metricName,
                                      Instant currentPeriodStart,
                                      Instant currentPeriodEnd,
                                      Instant previousPeriodStart,
                                      Instant previousPeriodEnd) {
        log.debug("Calculating trend for metric: {}", metricName);

        String currentQuery = """
            SELECT AVG(metric_value) as avg_value
            FROM performance_metrics
            WHERE metric_name = ?
            AND time >= ?
            AND time <= ?
 """;

        String previousQuery = """
            SELECT AVG(metric_value) as avg_value
            FROM performance_metrics
            WHERE metric_name = ?
            AND time >= ?
            AND time <= ?
 """;

        Double currentValue = jdbcTemplate.queryForObject(currentQuery, Double.class,
                metricName, currentPeriodStart, currentPeriodEnd);
        Double previousValue = jdbcTemplate.queryForObject(previousQuery, Double.class,
                metricName, previousPeriodStart, previousPeriodEnd);

        double changePercent = 0.0;
        if (previousValue != null && previousValue != 0) {
            changePercent = ((currentValue != null ? currentValue : 0) - previousValue) / previousValue * 100;
        }

        return MetricTrend.builder()
                .metricName(metricName)
                .currentValue(currentValue)
                .previousValue(previousValue)
                .changePercent(changePercent)
                .changeDirection(changePercent > 0 ? "INCREASE" : changePercent < 0 ? "DECREASE" : "NO_CHANGE")
                .periodStart(currentPeriodStart)
                .periodEnd(currentPeriodEnd)
                .build();
    }

    /**
     * Get multiple metrics at once
     */
    public Map<String, List<TimeSeriesDataPoint>> getMultipleMetrics(List<String> metricNames,
                                                                      Instant startTime,
                                                                      Instant endTime,
                                                                      String aggregationInterval) {
        log.debug("Fetching multiple metrics: {}", metricNames);

        Map<String, List<TimeSeriesDataPoint>> result = new HashMap<>();

        for (String metricName : metricNames) {
            result.put(metricName, getMetricData(metricName, startTime, endTime, aggregationInterval));
        }

        return result;
    }

    /**
     * Get top metrics by value in a time period
     */
    public List<MetricSummary> getTopMetrics(Instant startTime,
                                             Instant endTime,
                                             int limit) {
        log.debug("Getting top {} metrics from {} to {}", limit, startTime, endTime);

        String query = """
            SELECT
                metric_name,
                AVG(metric_value) as avg_value,
                MAX(metric_value) as max_value,
                COUNT(*) as sample_count
            FROM performance_metrics
            WHERE time >= ?
            AND time <= ?
            GROUP BY metric_name
            ORDER BY max_value DESC
            LIMIT ?
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, startTime, endTime, limit);

        return rows.stream()
                .map(row -> MetricSummary.builder()
                        .metricName(getString(row, "metric_name"))
                        .averageValue(getDouble(row, "avg_value"))
                        .maximumValue(getDouble(row, "max_value"))
                        .sampleCount(getLong(row, "sample_count"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get business metrics data
     */
    public List<BusinessMetricDataPoint> getBusinessMetricData(String metricName,
                                                                Instant startTime,
                                                                Instant endTime) {
        log.debug("Fetching business metric: {} from {} to {}", metricName, startTime, endTime);

        String query = """
            SELECT
                time_bucket('1 day', time) as bucket,
                SUM(metric_value) as total_value,
                AVG(metric_value) as avg_value,
                customer_id,
                product_id
            FROM business_metrics
            WHERE metric_name = ?
            AND time >= ?
            AND time <= ?
            GROUP BY bucket, customer_id, product_id
            ORDER BY bucket ASC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, metricName, startTime, endTime);

        return rows.stream()
                .map(this::mapToBusinessMetricDataPoint)
                .collect(Collectors.toList());
    }

    /**
     * Get resource metrics data
     */
    public List<ResourceMetricDataPoint> getResourceMetricData(String host,
                                                               String resourceType,
                                                               Instant startTime,
                                                               Instant endTime) {
        log.debug("Fetching resource metrics: {} - {} from {} to {}", host, resourceType, startTime, endTime);

        String query = """
            SELECT
                time_bucket('5 minutes', time) as bucket,
                AVG(usage_percent) as avg_usage_percent,
                MAX(usage_percent) as max_usage_percent,
                resource_name
            FROM resource_metrics
            WHERE host = ?
            AND resource_type = ?
            AND time >= ?
            AND time <= ?
            GROUP BY bucket, resource_name
            ORDER BY bucket ASC
 """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query, host, resourceType, startTime, endTime);

        return rows.stream()
                .map(this::mapToResourceMetricDataPoint)
                .collect(Collectors.toList());
    }

    /**
     * Get capacity planning data
     */
    public CapacityPlanningData getCapacityPlanningData(String resourceType,
                                                         Instant startTime,
                                                         Instant endTime) {
        log.debug("Getting capacity planning data for: {}", resourceType);

        String query = """
            SELECT
                AVG(usage_percent) as avg_usage,
                MAX(usage_percent) as max_usage,
                MIN(usage_percent) as min_usage
            FROM resource_metrics
            WHERE resource_type = ?
            AND time >= ?
            AND time <= ?
 """;

        Map<String, Object> row = jdbcTemplate.queryForMap(query, resourceType, startTime, endTime);

        return CapacityPlanningData.builder()
                .resourceType(resourceType)
                .averageUsage(getDouble(row, "avg_usage"))
                .maximumUsage(getDouble(row, "max_usage"))
                .minimumUsage(getDouble(row, "min_usage"))
                .capacityRecommendation(getCapacityRecommendation(getDouble(row, "max_usage")))
                .build();
    }

    /**
     * Insert a performance metric
     */
    public void insertPerformanceMetric(String metricName,
                                        Double metricValue,
                                        String metricUnit,
                                        Map<String, Object> tags) {
        log.debug("Inserting performance metric: {} = {}", metricName, metricValue);

        String query = "SELECT insert_performance_metric(?, ?, ?, ?)";
        jdbcTemplate.update(query, metricName, metricValue, metricUnit, tags);
    }

    /**
     * Insert a business metric
     */
    public void insertBusinessMetric(String metricName,
                                     Double metricValue,
                                     String metricUnit,
                                     UUID customerId,
                                     UUID productId,
                                     Map<String, Object> metadata) {
        log.debug("Inserting business metric: {} = {}", metricName, metricValue);

        String query = "SELECT insert_business_metric(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(query, metricName, metricValue, metricUnit, customerId, productId, metadata);
    }

    /**
     * Helper methods
     */
    private TimeSeriesDataPoint mapToTimeSeriesDataPoint(Map<String, Object> row) {
        return TimeSeriesDataPoint.builder()
                .timestamp(getInstant(row, "time"))
                .averageValue(getDouble(row, "avg_value"))
                .minimumValue(getDouble(row, "min_value"))
                .maximumValue(getDouble(row, "max_value"))
                .sampleCount(getLong(row, "sample_count"))
                .build();
    }

    private BusinessMetricDataPoint mapToBusinessMetricDataPoint(Map<String, Object> row) {
        return BusinessMetricDataPoint.builder()
                .timestamp(getInstant(row, "bucket"))
                .totalValue(getDouble(row, "total_value"))
                .averageValue(getDouble(row, "avg_value"))
                .customerId(getUUID(row, "customer_id"))
                .productId(getUUID(row, "product_id"))
                .build();
    }

    private ResourceMetricDataPoint mapToResourceMetricDataPoint(Map<String, Object> row) {
        return ResourceMetricDataPoint.builder()
                .timestamp(getInstant(row, "bucket"))
                .averageUsagePercent(getDouble(row, "avg_usage_percent"))
                .maximumUsagePercent(getDouble(row, "max_usage_percent"))
                .resourceName(getString(row, "resource_name"))
                .build();
    }

    private String getCapacityRecommendation(Double maxUsage) {
        if (maxUsage == null) return "UNKNOWN";
        if (maxUsage > 90) return "CRITICAL - Immediate action required";
        if (maxUsage > 80) return "HIGH - Plan capacity increase";
        if (maxUsage > 70) return "MEDIUM - Monitor closely";
        return "LOW - Healthy";
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Double getDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).doubleValue() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? ((Number) value).longValue() : null;
    }

    private Instant getInstant(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toInstant();
        } else if (value instanceof String) {
            return Instant.parse((String) value);
        }
        return null;
    }

    private UUID getUUID(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof String) {
            return UUID.fromString((String) value);
        }
        return (UUID) value;
    }
}
