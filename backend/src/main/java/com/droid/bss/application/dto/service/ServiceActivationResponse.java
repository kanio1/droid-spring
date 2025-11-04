package com.droid.bss.application.dto.service;

import com.droid.bss.domain.service.ServiceActivationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for Service Activation
 */
public record ServiceActivationResponse(
        String id,
        String customerId,
        String serviceCode,
        String serviceName,
        String status,
        LocalDateTime activationDate,
        LocalDateTime deactivationDate,
        LocalDateTime scheduledDate,
        String activationNotes,
        String deactivationNotes,
        String correlationId,
        Integer retryCount,
        Integer maxRetries,
        List<ServiceActivationStepResponse> steps
) {
    public static ServiceActivationResponse from(ServiceActivationEntity activation) {
        return new ServiceActivationResponse(
                activation.getId(),
                activation.getCustomer().getId(),
                activation.getService().getServiceCode(),
                activation.getService().getName(),
                activation.getStatus().name(),
                activation.getActivationDate(),
                activation.getDeactivationDate(),
                activation.getScheduledDate(),
                activation.getActivationNotes(),
                activation.getDeactivationNotes(),
                activation.getCorrelationId(),
                activation.getRetryCount(),
                activation.getMaxRetries(),
                activation.getSteps().stream()
                        .map(ServiceActivationStepResponse::from)
                        .collect(Collectors.toList())
        );
    }
}
