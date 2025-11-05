package com.droid.bss.application.command.service;

import com.droid.bss.application.dto.service.CreateServiceActivationCommand;
import com.droid.bss.application.dto.service.ServiceActivationResponse;
import com.droid.bss.domain.service.*;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.service.ServiceActivationStepEntity;
import com.droid.bss.domain.service.event.ServiceEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case for creating service activations
 */
@Service
public class CreateServiceActivationUseCase {

    private final ServiceActivationService service;
    private final ServiceActivationRepository activationRepository;
    private final ServiceActivationStepRepository stepRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceEventPublisher eventPublisher;

    public CreateServiceActivationUseCase(
            ServiceActivationService service,
            ServiceActivationRepository activationRepository,
            ServiceActivationStepRepository stepRepository,
            CustomerRepository customerRepository,
            ServiceRepository serviceRepository,
            ServiceEventPublisher eventPublisher) {
        this.service = service;
        this.activationRepository = activationRepository;
        this.stepRepository = stepRepository;
        this.customerRepository = customerRepository;
        this.serviceRepository = serviceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ServiceActivationResponse handle(CreateServiceActivationCommand command) {
        // Get customer and service
        Customer customer = customerRepository.findById(CustomerId.of(command.customerId()))
                .orElseThrow(() -> new RuntimeException("Customer not found: " + command.customerId()));

        ServiceEntity serviceEntity = serviceRepository.findActiveByServiceCode(command.serviceCode())
                .orElseThrow(() -> new RuntimeException("Service not found: " + command.serviceCode()));

        // Check eligibility
        var eligibility = service.checkActivationEligibility(command.customerId(), command.serviceCode());
        if (!eligibility.eligible()) {
            throw new IllegalStateException("Service activation not eligible: " + eligibility.reason());
        }

        // Create activation
        ServiceActivationEntity activation = new ServiceActivationEntity(
                CustomerEntity.from(customer),
                serviceEntity,
                command.scheduledDate() != null ? ActivationStatus.SCHEDULED : ActivationStatus.PENDING
        );

        activation.setCorrelationId(command.correlationId() != null
                ? command.correlationId()
                : "ACTIVATION-" + UUID.randomUUID());

        activation.setScheduledDate(command.scheduledDate());
        activation.setActivationNotes(command.activationNotes());

        // Create activation steps
        createActivationSteps(activation, serviceEntity);

        // Save
        ServiceActivationEntity saved = activationRepository.save(activation);

        // Publish activation created event
        eventPublisher.publishServiceActivated(serviceEntity, saved);

        return ServiceActivationResponse.from(saved);
    }

    private void createActivationSteps(ServiceActivationEntity activation, ServiceEntity service) {
        // Step 1: Validate service configuration
        ServiceActivationStepEntity step1 = new ServiceActivationStepEntity(
                1,
                "Validate Service Configuration",
                "Validate that the service can be provisioned for this customer",
                ServiceActivationStepStatus.PENDING
        );
        activation.addStep(step1);

        // Step 2: Check dependencies
        if (service.hasDependencies()) {
            ServiceActivationStepEntity step2 = new ServiceActivationStepEntity(
                    2,
                    "Verify Service Dependencies",
                    "Verify that all required service dependencies are active",
                    ServiceActivationStepStatus.PENDING
            );
            activation.addStep(step2);
        }

        // Step 3: Allocate resources
        ServiceActivationStepEntity step3 = new ServiceActivationStepEntity(
                service.hasDependencies() ? 3 : 2,
                "Allocate Resources",
                "Allocate required resources for the service",
                ServiceActivationStepStatus.PENDING
        );
        step3.setProvisioningSystem("ResourceManager");
        step3.setProvisioningCommand("allocate:" + service.getServiceCode());
        activation.addStep(step3);

        // Step 4: Provision service
        ServiceActivationStepEntity step4 = new ServiceActivationStepEntity(
                service.hasDependencies() ? 4 : 3,
                "Provision Service",
                "Execute service provisioning",
                ServiceActivationStepStatus.PENDING
        );
        step4.setProvisioningSystem("ProvisioningEngine");
        step4.setProvisioningCommand(service.getProvisioningScript() != null
                ? service.getProvisioningScript()
                : "provision:" + service.getServiceCode());
        activation.addStep(step4);

        // Step 5: Verify activation
        ServiceActivationStepEntity step5 = new ServiceActivationStepEntity(
                service.hasDependencies() ? 5 : 4,
                "Verify Service Activation",
                "Verify that the service is active and working",
                ServiceActivationStepStatus.PENDING
        );
        activation.addStep(step5);
    }
}
