package com.droid.bss.application.service;

import com.droid.bss.infrastructure.timeseries.SystemMetricsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SystemMetricsService {

    private final SystemMetricsRepository repository;

    public SystemMetricsService(SystemMetricsRepository repository) {
        this.repository = repository;
    }

    public void recordSystemMetric(String serviceName, Double cpuUsage, Double memoryUsage,
                                  Double requestRate, Double errorRate, Double latencyP99) {
        repository.recordSystemMetric(serviceName, cpuUsage, memoryUsage, requestRate, errorRate, latencyP99);
    }

    public List<SystemMetricsRepository.SystemMetricPoint> getSystemMetrics(
            String serviceName, Instant startTime, Instant endTime) {
        return repository.getSystemMetrics(serviceName, startTime, endTime);
    }

    public List<SystemMetricsRepository.SystemMetricPoint> getSystemMetrics(
            String serviceName, int hours) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(hours * 60L * 60);
        return repository.getSystemMetrics(serviceName, startTime, endTime);
    }

    public SystemHealthStatus getSystemHealthStatus() {
        return getSystemHealthStatus(1); // Last 1 hour
    }

    public SystemHealthStatus getSystemHealthStatus(int hours) {
        Instant startTime = Instant.now().minusSeconds(hours * 60L * 60);
        List<SystemMetricsRepository.SystemHealthData> healthData = repository.getSystemHealthStatus(startTime);

        SystemHealthStatus status = new SystemHealthStatus();
        status.setServices(healthData);
        status.setOverallHealth(calculateOverallHealth(healthData));
        status.setTimestamp(Instant.now());

        return status;
    }

    public List<SystemMetricsRepository.PerformanceTrend> getPerformanceTrends(
            String serviceName, int hours) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(hours * 60L * 60);
        return repository.getPerformanceTrends(serviceName, startTime, endTime);
    }

    public List<SystemMetricsRepository.ServiceComparison> getServiceComparison(int hours) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(hours * 60L * 60);
        return repository.getServiceComparison(startTime, endTime);
    }

    public List<SystemMetricsRepository.ServiceComparison> getServiceComparison() {
        return getServiceComparison(24); // Last 24 hours
    }

    private String calculateOverallHealth(List<SystemMetricsRepository.SystemHealthData> healthData) {
        if (healthData.isEmpty()) {
            return "UNKNOWN";
        }

        int healthyCount = 0;
        int warningCount = 0;
        int criticalCount = 0;

        for (SystemMetricsRepository.SystemHealthData data : healthData) {
            if (isHealthy(data)) {
                healthyCount++;
            } else if (isWarning(data)) {
                warningCount++;
            } else {
                criticalCount++;
            }
        }

        if (criticalCount > 0) {
            return "CRITICAL";
        } else if (warningCount > 0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    private boolean isHealthy(SystemMetricsRepository.SystemHealthData data) {
        return data.getAvgCpu() < 70.0 &&
               data.getAvgMemory() < 70.0 &&
               data.getAvgErrorRate() < 1.0 &&
               data.getMaxLatency() < 500.0;
    }

    private boolean isWarning(SystemMetricsRepository.SystemHealthData data) {
        return data.getAvgCpu() < 85.0 &&
               data.getAvgMemory() < 85.0 &&
               data.getAvgErrorRate() < 5.0 &&
               data.getMaxLatency() < 1000.0;
    }

    // Inner classes for DTOs
    public static class SystemHealthStatus {
        private List<SystemMetricsRepository.SystemHealthData> services;
        private String overallHealth;
        private Instant timestamp;

        // Getters and setters
        public List<SystemMetricsRepository.SystemHealthData> getServices() { return services; }
        public void setServices(List<SystemMetricsRepository.SystemHealthData> services) { this.services = services; }
        public String getOverallHealth() { return overallHealth; }
        public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
}
