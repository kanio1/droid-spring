package com.droid.bss.application.dto.address;

import jakarta.validation.constraints.NotBlank;

/**
 * Command for setting an address as primary
 */
public record SetPrimaryAddressCommand(
        @NotBlank(message = "Address ID is required")
        String addressId
) {}
