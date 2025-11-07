package com.droid.bss.infrastructure.write.monitoring;

import com.droid.bss.domain.monitoring.Alert;
import com.droid.bss.domain.monitoring.AlertRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of AlertRepository
 */
@Repository
public class AlertRepositoryImpl implements AlertRepository {

    private final SpringDataAlertRepository jpaRepository;

    public AlertRepositoryImpl(SpringDataAlertRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Alert save(Alert alert) {
        AlertEntity entity = AlertEntity.fromDomain(alert);
        AlertEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Alert> findById(Long id) {
        return jpaRepository.findById(id).map(AlertEntity::toDomain);
    }

    @Override
    public List<Alert> findByCustomerId(Long customerId) {
        return jpaRepository.findByCustomerId(customerId).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findByResourceId(Long resourceId) {
        return jpaRepository.findByResourceId(resourceId).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findByCustomerIdAndResourceId(Long customerId, Long resourceId) {
        return jpaRepository.findByCustomerIdAndResourceId(customerId, resourceId).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findByStatusIn(List<String> statuses) {
        return jpaRepository.findByStatusIn(statuses).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findBySeverity(String severity) {
        return jpaRepository.findBySeverity(severity).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findByTriggeredAtBetween(Instant startTime, Instant endTime) {
        return jpaRepository.findByTriggeredAtBetween(startTime, endTime).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Alert> findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
            Long customerId, Long resourceId, String metricType, List<String> statuses) {
        return jpaRepository.findByCustomerIdAndResourceIdAndMetricTypeAndStatusIn(
                        customerId, resourceId, metricType, statuses).stream()
                .map(AlertEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCustomerIdAndStatusIn(Long customerId, List<String> statuses) {
        return jpaRepository.countByCustomerIdAndStatusIn(customerId, statuses);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
