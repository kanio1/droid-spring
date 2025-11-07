package com.droid.bss.domain.monitoring;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for CustomerResourceConfiguration
 */
public interface CustomerResourceConfigurationRepository {

    Optional<CustomerResourceConfiguration> findById(Long id);

    List<CustomerResourceConfiguration> findByCustomerId(Long customerId);

    List<CustomerResourceConfiguration> findByCustomerIdAndResourceType(Long customerId, String resourceType);

    List<CustomerResourceConfiguration> findByResourceId(String resourceId);

    List<CustomerResourceConfiguration> findByStatus(String status);

    CustomerResourceConfiguration save(CustomerResourceConfiguration config);

    void deleteById(Long id);

    void deleteByCustomerIdAndResourceType(Long customerId, String resourceType);
}
