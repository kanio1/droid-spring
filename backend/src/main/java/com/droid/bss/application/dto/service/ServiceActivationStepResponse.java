package com.droid.bss.application.dto.service;

import com.droid.bss.domain.service.ServiceActivationStepEntity;

import java.time.LocalDateTime;

/**
 * Response DTO for Service Activation Step
 */
public record ServiceActivationStepResponse(
        String id,
        Integer stepOrder,
        String stepName,
        String stepDescription,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        Integer retryCount,
        Integer maxRetries,
        String provisioningSystem,
        String provisioningCommand,
        Long executionTimeMs
) {
    public static ServiceActivationStepResponse from(ServiceActivationStepEntity step) {
        return new ServiceActivationStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getStepName(),
                step.getStepDescription(),
                step.getStatus().name(),
                step.getStartedAt(),
                step.getCompletedAt(),
                step.getErrorMessage(),
                step.getRetryCount(),
                step.getMaxRetries(),
                step.getProvisioningSystem(),
                step.getProvisioningCommand(),
                step.getExecutionTimeMs()
        );
    }
}
