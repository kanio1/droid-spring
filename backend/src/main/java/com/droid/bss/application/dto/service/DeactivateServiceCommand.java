package com.droid.bss.application.dto.service;

import jakarta.validation.constraints.NotNull;

/**
 * Command to deactivate a service
 */
public record DeactivateServiceCommand(
        @NotNull
        String activationId,

        String deactivationNotes
) {
}
