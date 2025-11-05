package com.droid.bss.application.command.service;

import com.droid.bss.application.dto.service.DeactivateServiceCommand;
import com.droid.bss.application.dto.service.ServiceActivationResponse;
import com.droid.bss.domain.service.ActivationStatus;
import com.droid.bss.domain.service.ServiceActivationEntity;
import com.droid.bss.domain.service.ServiceActivationStepEntity;
import com.droid.bss.domain.service.event.ServiceEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for deactivating services
 */
@Service
public class DeactivateServiceUseCase {

    private final ServiceActivationService service;
    private final ServiceActivationRepository activationRepository;
    private final ServiceActivationStepRepository stepRepository;
    private final ServiceEventPublisher eventPublisher;

    public DeactivateServiceUseCase(
            ServiceActivationService service,
            ServiceActivationRepository activationRepository,
            ServiceActivationStepRepository stepRepository,
            ServiceEventPublisher eventPublisher) {
        this.service = service;
        this.activationRepository = activationRepository;
        this.stepRepository = stepRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ServiceActivationResponse handle(DeactivateServiceCommand command) {
        ServiceActivationEntity activation = service.getServiceActivation(command.activationId())
                .orElseThrow(() -> new RuntimeException("Activation not found: " + command.activationId()));

        if (!activation.isActive()) {
            throw new IllegalStateException("Service is not active: " + command.activationId());
        }

        // Publish deactivation started event
        eventPublisher.publishServiceDeactivated(
                activation.getService(),
                activation.getCustomer().getId(),
                command.reason() != null ? command.reason() : "Service deactivated by user"
        );

        // Update status to deprovisioning
        activation.setStatus(ActivationStatus.DEPROVISIONING);

        // Create deactivation steps
        createDeactivationSteps(activation);

        ServiceActivationEntity saved = activationRepository.save(activation);
        return ServiceActivationResponse.from(saved);
    }

    private void createDeactivationSteps(ServiceActivationEntity activation) {
        // Step 1: Stop service
        ServiceActivationStepEntity step1 = new ServiceActivationStepEntity(
                1,
                "Stop Service",
                "Stop the service gracefully",
                ServiceActivationStepStatus.PENDING
        );
        step1.setProvisioningSystem("ProvisioningEngine");
        step1.setProvisioningCommand("stop:" + activation.getService().getServiceCode());
        activation.addStep(step1);

        // Step 2: Deallocate resources
        ServiceActivationStepEntity step2 = new ServiceActivationStepEntity(
                2,
                "Deallocate Resources",
                "Deallocate allocated resources",
                ServiceActivationStepStatus.PENDING
        );
        step2.setProvisioningSystem("ResourceManager");
        step2.setProvisioningCommand("deallocate:" + activation.getService().getServiceCode());
        activation.addStep(step2);

        // Step 3: Final verification
        ServiceActivationStepEntity step3 = new ServiceActivationStepEntity(
                3,
                "Verify Deactivation",
                "Verify that the service is completely deactivated",
                ServiceActivationStepStatus.PENDING
        );
        activation.addStep(step3);

        // Update activation status
        activation.setStatus(ActivationStatus.DEPROVISIONING);
    }
}
