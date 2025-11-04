package com.droid.bss.application.dto.product;

import com.droid.bss.domain.product.ProductType;
import com.droid.bss.domain.product.ProductCategory;
import com.droid.bss.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Command for updating an existing product
 */
@Schema(name = "UpdateProductRequest", description = "Request to update an existing product")
public record UpdateProductCommand(
    @Schema(
        description = "Unique product identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    @NotNull(message = "Product ID is required")
    UUID id,

    @Schema(
        description = "Unique product code",
        example = "PROD-001",
        minLength = 3,
        maxLength = 50
    )
    @NotBlank(message = "Product code is required")
    @Size(min = 3, max = 50, message = "Product code must be between 3 and 50 characters")
    String productCode,

    @Schema(
        description = "Product name",
        example = "Premium Mobile Plan",
        minLength = 2,
        maxLength = 200
    )
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    String name,

    @Schema(
        description = "Product description",
        example = "Unlimited calls, SMS, and 50GB data per month",
        maxLength = 2000
    )
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    String description,

    @Schema(
        description = "Product type",
        example = "SERVICE",
        implementation = String.class
    )
    @NotNull(message = "Product type is required")
    ProductType productType,

    @Schema(
        description = "Product category",
        example = "MOBILE",
        implementation = String.class
    )
    ProductCategory category,

    @Schema(
        description = "Product price",
        example = "99.99",
        minimum = "0"
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    BigDecimal price,

    @Schema(
        description = "Currency code (ISO 4217)",
        example = "PLN",
        maxLength = 3
    )
    @Size(max = 3, message = "Currency must be 3 characters")
    String currency,

    @Schema(
        description = "Billing period",
        example = "MONTHLY",
        maxLength = 20
    )
    @NotBlank(message = "Billing period is required")
    @Size(max = 20, message = "Billing period cannot exceed 20 characters")
    String billingPeriod,

    @Schema(
        description = "Product status",
        example = "ACTIVE",
        implementation = String.class
    )
    @NotNull(message = "Status is required")
    ProductStatus status,

    @Schema(
        description = "Product validity start date",
        example = "2024-01-01",
        type = "string",
        format = "date"
    )
    LocalDate validityStart,

    @Schema(
        description = "Product validity end date",
        example = "2024-12-31",
        type = "string",
        format = "date"
    )
    LocalDate validityEnd,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    @NotNull(message = "Version is required for optimistic locking")
    Long version
) {
}
