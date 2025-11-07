package com.droid.bss.application.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stub class for ProductDto
 * Minimal implementation for testing purposes
 */
@Schema(name = "ProductDto", description = "Product data transfer object")
public record ProductDto(

    @Schema(description = "Product ID")
    String id,

    @Schema(description = "Product code", example = "PROD-2024-0001")
    String productCode,

    @Schema(description = "Product name", example = "Premium Service")
    String name,

    @Schema(description = "Product description")
    String description,

    @Schema(description = "Product type", example = "SERVICE")
    String productType,

    @Schema(description = "Product category", example = "SUBSCRIPTION")
    String category,

    @Schema(description = "Price")
    BigDecimal price,

    @Schema(description = "Currency code", example = "PLN")
    String currency,

    @Schema(description = "Billing period", example = "MONTHLY")
    String billingPeriod,

    @Schema(description = "Product status", example = "ACTIVE")
    String status,

    @Schema(description = "Validity start date")
    LocalDate validityStart,

    @Schema(description = "Validity end date")
    LocalDate validityEnd,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp")
    LocalDateTime updatedAt
) {
}
