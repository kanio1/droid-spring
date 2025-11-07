package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.ResourceThreshold;
import com.droid.bss.domain.monitoring.ResourceThresholdRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of ResourceThresholdRepository
 */
@Repository
public class ResourceThresholdRepositoryImpl implements ResourceThresholdRepository {

    private final SpringDataResourceThresholdRepository jpaRepository;

    public ResourceThresholdRepositoryImpl(SpringDataResourceThresholdRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ResourceThreshold save(ResourceThreshold threshold) {
        ResourceThresholdEntity entity = ResourceThresholdEntity.fromDomain(threshold);
        ResourceThresholdEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<ResourceThreshold> findById(Long id) {
        return jpaRepository.findById(id).map(ResourceThresholdEntity::toDomain);
    }

    @Override
    public List<ResourceThreshold> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(ResourceThresholdEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceThreshold> findByResourceId(Long resourceId) {
        return jpaRepository.findByResourceId(resourceId).stream()
                .map(ResourceThresholdEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceThreshold> findByCustomerIdAndResourceId(Long customerId, Long resourceId) {
        return jpaRepository.findByCustomerIdAndResourceId(customerId, resourceId).stream()
                .map(ResourceThresholdEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ResourceThreshold> findByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType) {
        return jpaRepository.findByCustomerIdAndResourceIdAndMetricType(customerId, resourceId, metricType)
                .map(ResourceThresholdEntity::toDomain);
    }

    @Override
    public List<ResourceThreshold> findByEnabledTrue() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(ResourceThresholdEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByCustomerIdAndResourceIdAndMetricType(
            Long customerId, Long resourceId, String metricType) {
        return jpaRepository.existsByCustomerIdAndResourceIdAndMetricType(customerId, resourceId, metricType);
    }
}
