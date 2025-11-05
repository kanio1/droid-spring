package com.droid.bss.application.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command for changing address status
 */
public record ChangeAddressStatusCommand(
        @NotBlank(message = "Address ID is required")
        String id,

        @NotNull(message = "Status is required")
        String status
) {}
