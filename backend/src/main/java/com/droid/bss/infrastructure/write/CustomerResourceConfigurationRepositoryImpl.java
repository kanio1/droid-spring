package com.droid.bss.infrastructure.write;

import com.droid.bss.domain.monitoring.CustomerResourceConfiguration;
import com.droid.bss.domain.monitoring.CustomerResourceConfigurationRepository;
import com.droid.bss.infrastructure.database.entity.CustomerResourceConfigurationEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CustomerResourceConfigurationRepositoryImpl implements CustomerResourceConfigurationRepository {

    private final SpringDataCustomerResourceConfigurationRepository springDataRepository;

    public CustomerResourceConfigurationRepositoryImpl(SpringDataCustomerResourceConfigurationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<CustomerResourceConfiguration> findById(Long id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<CustomerResourceConfiguration> findByCustomerId(Long customerId) {
        return springDataRepository.findByCustomerId(customerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResourceConfiguration> findByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return springDataRepository.findByCustomerIdAndResourceType(customerId, resourceType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResourceConfiguration> findByResourceId(String resourceId) {
        return springDataRepository.findByResourceId(resourceId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResourceConfiguration> findByStatus(String status) {
        return springDataRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResourceConfiguration save(CustomerResourceConfiguration config) {
        var entity = toEntity(config);
        var savedEntity = springDataRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public void deleteByCustomerIdAndResourceType(Long customerId, String resourceType) {
        springDataRepository.deleteByCustomerIdAndResourceType(customerId, resourceType);
    }

    private CustomerResourceConfiguration toDomain(CustomerResourceConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }
        var config = new CustomerResourceConfiguration();
        config.setId(entity.getId());
        config.setCustomerId(Long.valueOf(entity.getCustomerId()));
        config.setResourceType(entity.getResourceType());
        config.setResourceId(entity.getResourceId());
        config.setResourceName(entity.getResourceName());
        config.setRegion(entity.getRegion());
        config.setMaxLimit(entity.getMaxLimit());
        config.setWarningThreshold(entity.getWarningThreshold());
        config.setCriticalThreshold(entity.getCriticalThreshold());
        config.setBudgetLimit(entity.getBudgetLimit());
        config.setBudgetCurrency(entity.getBudgetCurrency());
        config.setAlertEmail(entity.getAlertEmail());
        config.setAlertPhone(entity.getAlertPhone());
        config.setAlertSlackWebhook(entity.getAlertSlackWebhook());
        config.setAutoScalingEnabled(entity.isAutoScalingEnabled());
        config.setScaleUpThreshold(entity.getScaleUpThreshold());
        config.setScaleDownThreshold(entity.getScaleDownThreshold());
        config.setStatus(entity.getStatus());
        config.setCreatedAt(entity.getCreatedAt());
        config.setUpdatedAt(entity.getUpdatedAt());
        config.setTags(entity.getTags());
        return config;
    }

    private CustomerResourceConfigurationEntity toEntity(CustomerResourceConfiguration domain) {
        if (domain == null) {
            return null;
        }
        var entity = new CustomerResourceConfigurationEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(String.valueOf(domain.getCustomerId()));
        entity.setResourceType(domain.getResourceType());
        entity.setResourceId(domain.getResourceId());
        entity.setResourceName(domain.getResourceName());
        entity.setRegion(domain.getRegion());
        entity.setMaxLimit(domain.getMaxLimit());
        entity.setWarningThreshold(domain.getWarningThreshold());
        entity.setCriticalThreshold(domain.getCriticalThreshold());
        entity.setBudgetLimit(domain.getBudgetLimit());
        entity.setBudgetCurrency(domain.getBudgetCurrency());
        entity.setAlertEmail(domain.getAlertEmail());
        entity.setAlertPhone(domain.getAlertPhone());
        entity.setAlertSlackWebhook(domain.getAlertSlackWebhook());
        entity.setAutoScalingEnabled(domain.isAutoScalingEnabled());
        entity.setScaleUpThreshold(domain.getScaleUpThreshold());
        entity.setScaleDownThreshold(domain.getScaleDownThreshold());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setTags(domain.getTags());
        return entity;
    }
}
