package com.droid.bss.application.service;

import com.droid.bss.infrastructure.timeseries.CustomerMetricsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomerMetricsService {

    private final CustomerMetricsRepository repository;

    public CustomerMetricsService(CustomerMetricsRepository repository) {
        this.repository = repository;
    }

    public void recordCustomerMetric(UUID customerId, String metricName,
                                    Double metricValue, Map<String, Object> labels) {
        repository.recordCustomerMetric(customerId, metricName, metricValue, labels);
    }

    public List<CustomerMetricsRepository.CustomerMetric> getCustomerMetrics(
            UUID customerId, Instant startTime, Instant endTime) {
        return repository.getCustomerMetrics(customerId, startTime, endTime);
    }

    public List<CustomerMetricsRepository.MetricAggregate> getMetricAggregates(
            String metricName, String period, Instant startTime, Instant endTime) {
        return repository.getMetricAggregates(metricName, period, startTime, endTime);
    }

    public List<CustomerMetricsRepository.CustomerActivity> getTopCustomers(
            Instant startTime, Instant endTime, int limit) {
        return repository.getTopCustomers(startTime, endTime, limit);
    }

    public List<CustomerMetricsRepository.CustomerActivity> getTopCustomers(int days, int limit) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getTopCustomers(startTime, endTime, limit);
    }

    public List<CustomerMetricsRepository.MetricAggregate> getDailyMetrics(
            String metricName, int days) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(days * 24L * 60 * 60);
        return repository.getMetricAggregates(metricName, "day", startTime, endTime);
    }

    public List<CustomerMetricsRepository.MetricAggregate> getHourlyMetrics(
            String metricName, int hours) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(hours * 60L * 60);
        return repository.getMetricAggregates(metricName, "hour", startTime, endTime);
    }
}
