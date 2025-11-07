package com.droid.bss.infrastructure.timeseries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for customer time-series metrics stored in TimescaleDB
 */
@Repository
public class CustomerMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public CustomerMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record a customer metric
     */
    public void recordCustomerMetric(UUID customerId, String metricName,
                                    Double metricValue, Map<String, Object> labels) {
        String sql = "SELECT record_customer_metric(?, ?, ?, ?::jsonb)";

        String labelsJson = labels != null ? toJsonString(labels) : null;
        jdbcTemplate.update(sql, customerId, metricName, metricValue, labelsJson);
    }

    /**
     * Get customer metrics for a time range
     */
    public List<CustomerMetric> getCustomerMetrics(UUID customerId,
                                                   Instant startTime,
                                                   Instant endTime) {
        String sql = "SELECT time, customer_id, metric_name, metric_value, labels " +
                     "FROM customer_metrics " +
                     "WHERE customer_id = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "ORDER BY time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{customerId, java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                CustomerMetric metric = new CustomerMetric();
                metric.setTime(rs.getTimestamp("time").toInstant());
                metric.setCustomerId(rs.getObject("customer_id", UUID.class));
                metric.setMetricName(rs.getString("metric_name"));
                metric.setMetricValue(rs.getDouble("metric_value"));
                String labelsJson = rs.getString("labels");
                if (labelsJson != null) {
                    metric.setLabels(parseJson(labelsJson));
                }
                return metric;
            });
    }

    /**
     * Get aggregate metrics by name for a time range
     */
    public List<MetricAggregate> getMetricAggregates(String metricName, String period,
                                                     Instant startTime, Instant endTime) {
        String sql = "SELECT time_bucket(?, time) AS bucket, " +
                     "AVG(metric_value) AS avg_value, " +
                     "MIN(metric_value) AS min_value, " +
                     "MAX(metric_value) AS max_value, " +
                     "COUNT(*) AS count " +
                     "FROM customer_metrics " +
                     "WHERE metric_name = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "GROUP BY bucket " +
                     "ORDER BY bucket DESC";

        String bucketInterval = getBucketInterval(period);

        return jdbcTemplate.query(sql,
            new Object[]{bucketInterval, metricName,
                        java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                MetricAggregate aggregate = new MetricAggregate();
                aggregate.setBucket(rs.getTimestamp("bucket").toInstant());
                aggregate.setAvgValue(rs.getDouble("avg_value"));
                aggregate.setMinValue(rs.getDouble("min_value"));
                aggregate.setMaxValue(rs.getDouble("max_value"));
                aggregate.setCount(rs.getLong("count"));
                return aggregate;
            });
    }

    /**
     * Get top customers by activity count
     */
    public List<CustomerActivity> getTopCustomers(Instant startTime, Instant endTime, int limit) {
        String sql = "SELECT customer_id, COUNT(*) AS activity_count, " +
                     "AVG(metric_value) AS avg_metric_value " +
                     "FROM customer_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY customer_id " +
                     "ORDER BY activity_count DESC " +
                     "LIMIT ?";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime), limit},
            (rs, rowNum) -> {
                CustomerActivity activity = new CustomerActivity();
                activity.setCustomerId(rs.getObject("customer_id", UUID.class));
                activity.setActivityCount(rs.getLong("activity_count"));
                activity.setAvgMetricValue(rs.getDouble("avg_metric_value"));
                return activity;
            });
    }

    private String getBucketInterval(String period) {
        return switch (period.toLowerCase()) {
            case "minute" -> "1 minute";
            case "hour" -> "1 hour";
            case "day" -> "1 day";
            case "week" -> "1 week";
            case "month" -> "1 month";
            default -> "1 day";
        };
    }

    private String toJsonString(Map<String, Object> map) {
        // Simple JSON conversion - in production, use a JSON library
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (sb.length() > 1) sb.append(", ");
            sb.append("\"").append(k).append("\":");
            if (v instanceof String) {
                sb.append("\"").append(v).append("\"");
            } else {
                sb.append(v);
            }
        });
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // Simple JSON parsing - in production, use a JSON library like Jackson
        return Map.of(); // Placeholder
    }

    // Inner classes for DTOs
    public static class CustomerMetric {
        private Instant time;
        private UUID customerId;
        private String metricName;
        private Double metricValue;
        private Map<String, Object> labels;

        // Getters and setters
        public Instant getTime() { return time; }
        public void setTime(Instant time) { this.time = time; }
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public Double getMetricValue() { return metricValue; }
        public void setMetricValue(Double metricValue) { this.metricValue = metricValue; }
        public Map<String, Object> getLabels() { return labels; }
        public void setLabels(Map<String, Object> labels) { this.labels = labels; }
    }

    public static class MetricAggregate {
        private Instant bucket;
        private Double avgValue;
        private Double minValue;
        private Double maxValue;
        private Long count;

        // Getters and setters
        public Instant getBucket() { return bucket; }
        public void setBucket(Instant bucket) { this.bucket = bucket; }
        public Double getAvgValue() { return avgValue; }
        public void setAvgValue(Double avgValue) { this.avgValue = avgValue; }
        public Double getMinValue() { return minValue; }
        public void setMinValue(Double minValue) { this.minValue = minValue; }
        public Double getMaxValue() { return maxValue; }
        public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
    }

    public static class CustomerActivity {
        private UUID customerId;
        private Long activityCount;
        private Double avgMetricValue;

        // Getters and setters
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        public Long getActivityCount() { return activityCount; }
        public void setActivityCount(Long activityCount) { this.activityCount = activityCount; }
        public Double getAvgMetricValue() { return avgMetricValue; }
        public void setAvgMetricValue(Double avgMetricValue) { this.avgMetricValue = avgMetricValue; }
    }
}
