package com.droid.bss.application.command.service;

import com.droid.bss.domain.service.*;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing service activations
 */
@Service
public class ServiceActivationService {

    private final ServiceRepository serviceRepository;
    private final ServiceActivationRepository activationRepository;
    private final ServiceActivationStepRepository stepRepository;
    private final CustomerRepository customerRepository;

    public ServiceActivationService(
            ServiceRepository serviceRepository,
            ServiceActivationRepository activationRepository,
            ServiceActivationStepRepository stepRepository,
            CustomerRepository customerRepository) {
        this.serviceRepository = serviceRepository;
        this.activationRepository = activationRepository;
        this.stepRepository = stepRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Find all available services
     */
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAllActive();
    }

    /**
     * Find services by category
     */
    public List<ServiceEntity> getServicesByCategory(String category) {
        return serviceRepository.findByCategory(category);
    }

    /**
     * Find services by type
     */
    public List<ServiceEntity> getServicesByType(ServiceType serviceType) {
        return serviceRepository.findByServiceType(serviceType);
    }

    /**
     * Get active service activations for a customer
     */
    public List<ServiceActivationEntity> getCustomerActiveActivations(String customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return activationRepository.findActiveByCustomer(customer);
    }

    /**
     * Get service activation by ID
     */
    public Optional<ServiceActivationEntity> getServiceActivation(String activationId) {
        return activationRepository.findById(activationId);
    }

    /**
     * Check if a service can be activated for a customer
     * (checks dependencies and conflicts)
     */
    public ActivationEligibility checkActivationEligibility(String customerId, String serviceCode) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        ServiceEntity service = serviceRepository.findActiveByServiceCode(serviceCode)
                .orElseThrow(() -> new RuntimeException("Service not found: " + serviceCode));

        // Check if service is already active
        Optional<ServiceActivationEntity> existing = activationRepository.findByCustomerAndService(customer, service);
        if (existing.isPresent() && existing.get().isActive()) {
            return new ActivationEligibility(false, "Service is already active for this customer");
        }

        // Check dependencies
        Set<String> dependencies = service.getDependsOnServiceCodes();
        for (String depCode : dependencies) {
            Optional<ServiceEntity> depServiceOpt = serviceRepository.findActiveByServiceCode(depCode);
            if (depServiceOpt.isEmpty()) {
                return new ActivationEligibility(false, "Dependency service not found: " + depCode);
            }

            List<ServiceActivationEntity> depActivations = activationRepository.findActiveByCustomer(customer);
            boolean hasDep = depActivations.stream()
                    .anyMatch(a -> depCode.equals(a.getService().getServiceCode()));

            if (!hasDep) {
                return new ActivationEligibility(false, "Required dependency not active: " + depCode);
            }
        }

        return new ActivationEligibility(true, null);
    }

    /**
     * Calculate activation order based on dependencies
     */
    public List<ServiceEntity> calculateActivationOrder(List<ServiceEntity> services) {
        return services.stream()
                .sorted((s1, s2) -> {
                    // Services with fewer dependencies come first
                    int deps1 = s1.getDependsOnServiceCodes().size();
                    int deps2 = s2.getDependsOnServiceCodes().size();
                    return Integer.compare(deps1, deps2);
                })
                .collect(Collectors.toList());
    }
}
