package com.droid.bss.application.command.service;

import com.droid.bss.domain.service.*;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.service.event.ServiceEventPublisher;
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
    private final ServiceEventPublisher eventPublisher;

    public ServiceActivationService(
            ServiceRepository serviceRepository,
            ServiceActivationRepository activationRepository,
            ServiceActivationStepRepository stepRepository,
            CustomerRepository customerRepository,
            ServiceEventPublisher eventPublisher) {
        this.serviceRepository = serviceRepository;
        this.activationRepository = activationRepository;
        this.stepRepository = stepRepository;
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
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
        Customer customer = customerRepository.findById(CustomerId.of(customerId))
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));
        return activationRepository.findActiveByCustomer(customer);
    }

    /**
     * Get service activation by ID
     */
    public Optional<ServiceActivationEntity> getServiceActivation(String activationId) {
        return activationRepository.findById(java.util.UUID.fromString(activationId));
    }

    /**
     * Check if a service can be activated for a customer
     * (checks dependencies and conflicts)
     */
    public ActivationEligibility checkActivationEligibility(String customerId, String serviceCode) {
        Customer customer = customerRepository.findById(CustomerId.of(customerId))
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

    /**
     * Update activation status and publish event
     */
    @Transactional
    public void updateActivationStatus(String activationId, ActivationStatus newStatus, String errorMessage) {
        ServiceActivationEntity activation = activationRepository.findById(java.util.UUID.fromString(activationId))
                .orElseThrow(() -> new RuntimeException("Activation not found: " + activationId));

        ActivationStatus previousStatus = activation.getStatus();
        activation.setStatus(newStatus);

        // Publish status change event
        eventPublisher.publishServiceActivationStatusChanged(
                activation.getService(),
                activation,
                previousStatus
        );

        // Handle specific status events
        switch (newStatus) {
            case ACTIVE:
                activation.setActivationDate(LocalDateTime.now());
                eventPublisher.publishServiceActivationCompleted(activation.getService(), activation);
                break;
            case FAILED:
                eventPublisher.publishServiceActivationFailed(activation.getService(), activation, errorMessage);
                break;
            case PROVISIONING:
                eventPublisher.publishServiceActivated(activation.getService(), activation);
                break;
            case INACTIVE:
                eventPublisher.publishServiceDeactivated(
                        activation.getService(),
                        activation.getCustomer().getId(),
                        "Service deactivated"
                );
                break;
        }

        activationRepository.save(activation);
    }

    /**
     * Retry failed activation
     */
    @Transactional
    public void retryActivation(String activationId) {
        ServiceActivationEntity activation = activationRepository.findById(java.util.UUID.fromString(activationId))
                .orElseThrow(() -> new RuntimeException("Activation not found: " + activationId));

        if (activation.getRetryCount() >= activation.getMaxRetries()) {
            throw new IllegalStateException("Maximum retries reached for activation: " + activationId);
        }

        // Reset steps
        activation.getSteps().forEach(step -> {
            step.setStatus(ServiceActivationStepStatus.PENDING);
            step.setStartedAt(null);
            step.setCompletedAt(null);
            step.setErrorMessage(null);
        });

        // Increment retry count
        activation.setRetryCount(activation.getRetryCount() + 1);
        activation.setStatus(ActivationStatus.PENDING);

        activationRepository.save(activation);
    }
}
