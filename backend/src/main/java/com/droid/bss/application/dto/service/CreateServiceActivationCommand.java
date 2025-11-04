package com.droid.bss.application.dto.service;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Command to create a service activation
 */
public record CreateServiceActivationCommand(
        @NotNull
        String customerId,

        @NotNull
        String serviceCode,

        String correlationId,

        LocalDateTime scheduledDate,

        String activationNotes
) {
}
