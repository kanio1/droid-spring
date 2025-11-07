package com.droid.bss.application.service.monitoring;

import com.droid.bss.application.dto.monitoring.MetricResponse;
import com.droid.bss.domain.monitoring.ResourceMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for retrieving resource metrics
 */
@Service
@Transactional(readOnly = true)
public class GetMetricsUseCase {

    private final ResourceMetricRepository metricRepository;

    public GetMetricsUseCase(ResourceMetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    /**
     * Get metrics by customer ID and time range
     */
    public List<MetricResponse> getMetricsByCustomerAndTimeRange(
            Long customerId, Instant startTime, Instant endTime) {
        return metricRepository.findByCustomerIdAndTimestampBetween(customerId, startTime, endTime)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get metrics by resource ID and time range
     */
    public List<MetricResponse> getMetricsByResourceAndTimeRange(
            Long resourceId, Instant startTime, Instant endTime) {
        return metricRepository.findByResourceIdAndTimestampBetween(resourceId, startTime, endTime)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get metrics by customer ID, resource ID and time range
     */
    public List<MetricResponse> getMetricsByCustomerResourceAndTimeRange(
            Long customerId, Long resourceId, Instant startTime, Instant endTime) {
        return metricRepository.findByCustomerIdAndResourceIdAndTimestampBetween(
                        customerId, resourceId, startTime, endTime)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get latest metrics for a resource
     */
    public List<MetricResponse> getLatestMetrics(Long resourceId, int limit) {
        return metricRepository.findLatestByResourceId(resourceId, limit)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MetricResponse toResponse(com.droid.bss.domain.monitoring.ResourceMetric metric) {
        return new MetricResponse(
                metric.getId(),
                metric.getCustomerId(),
                metric.getResourceId(),
                metric.getMetricType(),
                metric.getValue(),
                metric.getUnit(),
                metric.getTimestamp(),
                metric.getSource()
        );
    }
}
