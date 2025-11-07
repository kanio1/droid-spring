package com.droid.bss.application.query.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Stub class for GetAddressesQuery
 * Minimal implementation for testing purposes
 */
@Schema(name = "GetAddressesQuery", description = "Query to get addresses")
public record GetAddressesQuery(

    @Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String customerId
) {
}
