package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.ResourceMetric;
import com.droid.bss.domain.monitoring.ResourceMetricRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of ResourceMetricRepository
 */
@Repository
public class ResourceMetricRepositoryImpl implements ResourceMetricRepository {

    private final SpringDataResourceMetricRepository jpaRepository;

    public ResourceMetricRepositoryImpl(SpringDataResourceMetricRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ResourceMetric save(ResourceMetric metric) {
        ResourceMetricEntity entity = ResourceMetricEntity.fromDomain(metric);
        ResourceMetricEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void saveAll(List<ResourceMetric> metrics) {
        List<ResourceMetricEntity> entities = metrics.stream()
                .map(ResourceMetricEntity::fromDomain)
                .collect(Collectors.toList());
        jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<ResourceMetric> findById(Long id) {
        return jpaRepository.findById(id).map(ResourceMetricEntity::toDomain);
    }

    @Override
    public List<ResourceMetric> findByCustomerIdAndTimestampBetween(
            Long customerId, Instant startTime, Instant endTime) {
        return jpaRepository.findByCustomerIdAndTimestampBetween(customerId, startTime, endTime)
                .stream()
                .map(ResourceMetricEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceMetric> findByResourceIdAndTimestampBetween(
            Long resourceId, Instant startTime, Instant endTime) {
        return jpaRepository.findByResourceIdAndTimestampBetween(resourceId, startTime, endTime)
                .stream()
                .map(ResourceMetricEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceMetric> findByCustomerIdAndResourceIdAndTimestampBetween(
            Long customerId, Long resourceId, Instant startTime, Instant endTime) {
        return jpaRepository.findByCustomerIdAndResourceIdAndTimestampBetween(
                customerId, resourceId, startTime, endTime)
                .stream()
                .map(ResourceMetricEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceMetric> findLatestByResourceId(Long resourceId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return jpaRepository.findTopByResourceIdOrderByTimestampDesc(resourceId, pageable)
                .stream()
                .map(ResourceMetricEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTimestampBefore(Instant cutoffTime) {
        jpaRepository.deleteByTimestampBefore(cutoffTime);
    }
}
