package com.droid.bss.application.query.monitoring;

import com.droid.bss.domain.monitoring.CustomerResourceConfiguration;
import com.droid.bss.domain.monitoring.CustomerResourceConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetCustomerResourceConfigurationsUseCase {

    private final CustomerResourceConfigurationRepository configurationRepository;

    public GetCustomerResourceConfigurationsUseCase(CustomerResourceConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public List<CustomerResourceConfiguration> getByCustomerId(Long customerId) {
        return configurationRepository.findByCustomerId(customerId);
    }

    public List<CustomerResourceConfiguration> getByCustomerIdAndResourceType(Long customerId, String resourceType) {
        return configurationRepository.findByCustomerIdAndResourceType(customerId, resourceType);
    }

    public List<CustomerResourceConfiguration> getByResourceId(String resourceId) {
        return configurationRepository.findByResourceId(resourceId);
    }

    public List<CustomerResourceConfiguration> getByStatus(String status) {
        return configurationRepository.findByStatus(status);
    }
}
