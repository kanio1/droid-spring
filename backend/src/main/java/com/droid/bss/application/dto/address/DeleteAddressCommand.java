package com.droid.bss.application.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Stub class for DeleteAddressCommand
 * Minimal implementation for testing purposes
 */
@Schema(name = "DeleteAddressCommand", description = "Command to delete an address")
public record DeleteAddressCommand(

    @Schema(description = "Address ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String addressId
) {
}
