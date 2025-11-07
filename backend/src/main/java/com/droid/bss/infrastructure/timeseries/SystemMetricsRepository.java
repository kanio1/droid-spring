package com.droid.bss.infrastructure.timeseries;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for system performance time-series metrics stored in TimescaleDB
 */
@Repository
public class SystemMetricsRepository {

    private final JdbcTemplate jdbcTemplate;

    public SystemMetricsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Record a system metric
     */
    public void recordSystemMetric(String serviceName, Double cpuUsage, Double memoryUsage,
                                  Double requestRate, Double errorRate, Double latencyP99) {
        String sql = "SELECT record_system_metric(?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, serviceName, cpuUsage, memoryUsage, requestRate, errorRate, latencyP99);
    }

    /**
     * Get system metrics for a service
     */
    public List<SystemMetricPoint> getSystemMetrics(String serviceName, Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "time, " +
                     "service_name, " +
                     "cpu_usage, " +
                     "memory_usage, " +
                     "request_rate, " +
                     "error_rate, " +
                     "latency_p99 " +
                     "FROM system_metrics " +
                     "WHERE service_name = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "ORDER BY time DESC";

        return jdbcTemplate.query(sql,
            new Object[]{serviceName, java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                SystemMetricPoint point = new SystemMetricPoint();
                point.setTime(rs.getTimestamp("time").toInstant());
                point.setServiceName(rs.getString("service_name"));
                point.setCpuUsage(rs.getDouble("cpu_usage"));
                point.setMemoryUsage(rs.getDouble("memory_usage"));
                point.setRequestRate(rs.getDouble("request_rate"));
                point.setErrorRate(rs.getDouble("error_rate"));
                point.setLatencyP99(rs.getDouble("latency_p99"));
                return point;
            });
    }

    /**
     * Get system health status
     */
    public List<SystemHealthData> getSystemHealthStatus(Instant startTime) {
        String sql = "SELECT " +
                     "service_name, " +
                     "AVG(cpu_usage) as avg_cpu, " +
                     "AVG(memory_usage) as avg_memory, " +
                     "AVG(error_rate) as avg_error_rate, " +
                     "MAX(latency_p99) as max_latency " +
                     "FROM system_metrics " +
                     "WHERE time > ? " +
                     "GROUP BY service_name";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime)},
            (rs, rowNum) -> {
                SystemHealthData data = new SystemHealthData();
                data.setServiceName(rs.getString("service_name"));
                data.setAvgCpu(rs.getDouble("avg_cpu"));
                data.setAvgMemory(rs.getDouble("avg_memory"));
                data.setAvgErrorRate(rs.getDouble("avg_error_rate"));
                data.setMaxLatency(rs.getDouble("max_latency"));
                return data;
            });
    }

    /**
     * Get performance trends by service
     */
    public List<PerformanceTrend> getPerformanceTrends(String serviceName, Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "time_bucket('1 hour', time) AS hour, " +
                     "AVG(cpu_usage) AS avg_cpu, " +
                     "MAX(cpu_usage) AS max_cpu, " +
                     "AVG(memory_usage) AS avg_memory, " +
                     "AVG(request_rate) AS avg_request_rate, " +
                     "AVG(error_rate) AS avg_error_rate " +
                     "FROM system_metrics " +
                     "WHERE service_name = ? " +
                     "AND time BETWEEN ? AND ? " +
                     "GROUP BY hour " +
                     "ORDER BY hour DESC";

        return jdbcTemplate.query(sql,
            new Object[]{serviceName, java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                PerformanceTrend trend = new PerformanceTrend();
                trend.setHour(rs.getTimestamp("hour").toInstant());
                trend.setAvgCpu(rs.getDouble("avg_cpu"));
                trend.setMaxCpu(rs.getDouble("max_cpu"));
                trend.setAvgMemory(rs.getDouble("avg_memory"));
                trend.setAvgRequestRate(rs.getDouble("avg_request_rate"));
                trend.setAvgErrorRate(rs.getDouble("avg_error_rate"));
                return trend;
            });
    }

    /**
     * Get service comparison metrics
     */
    public List<ServiceComparison> getServiceComparison(Instant startTime, Instant endTime) {
        String sql = "SELECT " +
                     "service_name, " +
                     "AVG(cpu_usage) AS avg_cpu, " +
                     "AVG(memory_usage) AS avg_memory, " +
                     "AVG(request_rate) AS avg_request_rate, " +
                     "AVG(error_rate) AS avg_error_rate, " +
                     "AVG(latency_p99) AS avg_latency " +
                     "FROM system_metrics " +
                     "WHERE time BETWEEN ? AND ? " +
                     "GROUP BY service_name " +
                     "ORDER BY avg_error_rate DESC";

        return jdbcTemplate.query(sql,
            new Object[]{java.sql.Timestamp.from(startTime), java.sql.Timestamp.from(endTime)},
            (rs, rowNum) -> {
                ServiceComparison comparison = new ServiceComparison();
                comparison.setServiceName(rs.getString("service_name"));
                comparison.setAvgCpu(rs.getDouble("avg_cpu"));
                comparison.setAvgMemory(rs.getDouble("avg_memory"));
                comparison.setAvgRequestRate(rs.getDouble("avg_request_rate"));
                comparison.setAvgErrorRate(rs.getDouble("avg_error_rate"));
                comparison.setAvgLatency(rs.getDouble("avg_latency"));
                return comparison;
            });
    }

    // Inner classes for DTOs
    public static class SystemMetricPoint {
        private Instant time;
        private String serviceName;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double requestRate;
        private Double errorRate;
        private Double latencyP99;

        // Getters and setters
        public Instant getTime() { return time; }
        public void setTime(Instant time) { this.time = time; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        public Double getRequestRate() { return requestRate; }
        public void setRequestRate(Double requestRate) { this.requestRate = requestRate; }
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
        public Double getLatencyP99() { return latencyP99; }
        public void setLatencyP99(Double latencyP99) { this.latencyP99 = latencyP99; }
    }

    public static class SystemHealthData {
        private String serviceName;
        private Double avgCpu;
        private Double avgMemory;
        private Double avgErrorRate;
        private Double maxLatency;

        // Getters and setters
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Double getAvgCpu() { return avgCpu; }
        public void setAvgCpu(Double avgCpu) { this.avgCpu = avgCpu; }
        public Double getAvgMemory() { return avgMemory; }
        public void setAvgMemory(Double avgMemory) { this.avgMemory = avgMemory; }
        public Double getAvgErrorRate() { return avgErrorRate; }
        public void setAvgErrorRate(Double avgErrorRate) { this.avgErrorRate = avgErrorRate; }
        public Double getMaxLatency() { return maxLatency; }
        public void setMaxLatency(Double maxLatency) { this.maxLatency = maxLatency; }
    }

    public static class PerformanceTrend {
        private Instant hour;
        private Double avgCpu;
        private Double maxCpu;
        private Double avgMemory;
        private Double avgRequestRate;
        private Double avgErrorRate;

        // Getters and setters
        public Instant getHour() { return hour; }
        public void setHour(Instant hour) { this.hour = hour; }
        public Double getAvgCpu() { return avgCpu; }
        public void setAvgCpu(Double avgCpu) { this.avgCpu = avgCpu; }
        public Double getMaxCpu() { return maxCpu; }
        public void setMaxCpu(Double maxCpu) { this.maxCpu = maxCpu; }
        public Double getAvgMemory() { return avgMemory; }
        public void setAvgMemory(Double avgMemory) { this.avgMemory = avgMemory; }
        public Double getAvgRequestRate() { return avgRequestRate; }
        public void setAvgRequestRate(Double avgRequestRate) { this.avgRequestRate = avgRequestRate; }
        public Double getAvgErrorRate() { return avgErrorRate; }
        public void setAvgErrorRate(Double avgErrorRate) { this.avgErrorRate = avgErrorRate; }
    }

    public static class ServiceComparison {
        private String serviceName;
        private Double avgCpu;
        private Double avgMemory;
        private Double avgRequestRate;
        private Double avgErrorRate;
        private Double avgLatency;

        // Getters and setters
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Double getAvgCpu() { return avgCpu; }
        public void setAvgCpu(Double avgCpu) { this.avgCpu = avgCpu; }
        public Double getAvgMemory() { return avgMemory; }
        public void setAvgMemory(Double avgMemory) { this.avgMemory = avgMemory; }
        public Double getAvgRequestRate() { return avgRequestRate; }
        public void setAvgRequestRate(Double avgRequestRate) { this.avgRequestRate = avgRequestRate; }
        public Double getAvgErrorRate() { return avgErrorRate; }
        public void setAvgErrorRate(Double avgErrorRate) { this.avgErrorRate = avgErrorRate; }
        public Double getAvgLatency() { return avgLatency; }
        public void setAvgLatency(Double avgLatency) { this.avgLatency = avgLatency; }
    }
}
