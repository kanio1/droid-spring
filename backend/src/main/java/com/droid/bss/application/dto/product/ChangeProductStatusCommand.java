package com.droid.bss.application.dto.product;

import com.droid.bss.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command for changing product status
 */
@Schema(name = "ChangeProductStatusRequest", description = "Request to change product status")
public record ChangeProductStatusCommand(
    @Schema(
        description = "Unique product identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Product ID is required")
    UUID id,

    @Schema(
        description = "New product status",
        example = "ACTIVE",
        implementation = String.class
    )
    @NotNull(message = "Status is required")
    ProductStatus status,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
