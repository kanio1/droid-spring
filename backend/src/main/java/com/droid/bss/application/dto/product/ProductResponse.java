package com.droid.bss.application.dto.product;

import com.droid.bss.domain.product.ProductEntity;
import com.droid.bss.domain.product.ProductType;
import com.droid.bss.domain.product.ProductCategory;
import com.droid.bss.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Product entity
 */
@Schema(name = "ProductResponse", description = "Product response with full details")
public record ProductResponse(
    @Schema(
        description = "Unique product identifier",
        example = "123e4567-e89b-12d3-a456-426614174000",
        type = "string",
        format = "uuid"
    )
    UUID id,

    @Schema(
        description = "Unique product code",
        example = "PROD-001"
    )
    String productCode,

    @Schema(
        description = "Product name",
        example = "Premium Mobile Plan"
    )
    String name,

    @Schema(
        description = "Product description",
        example = "Unlimited calls, SMS, and 50GB data per month"
    )
    String description,

    @Schema(
        description = "Product type code",
        example = "SERVICE"
    )
    String productType,

    @Schema(
        description = "Product type display name",
        example = "Usługa"
    )
    String productTypeDisplayName,

    @Schema(
        description = "Product category code",
        example = "MOBILE"
    )
    String category,

    @Schema(
        description = "Product category display name",
        example = "Mobilny"
    )
    String categoryDisplayName,

    @Schema(
        description = "Product price",
        example = "99.99"
    )
    BigDecimal price,

    @Schema(
        description = "Currency code",
        example = "PLN"
    )
    String currency,

    @Schema(
        description = "Billing period",
        example = "MONTHLY"
    )
    String billingPeriod,

    @Schema(
        description = "Product status code",
        example = "ACTIVE"
    )
    String status,

    @Schema(
        description = "Product status display name",
        example = "Aktywny"
    )
    String statusDisplayName,

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
        description = "Whether the product is currently active (within validity period)",
        example = "true"
    )
    boolean isActive,

    @Schema(
        description = "Timestamp when the product was created",
        example = "2024-01-01T10:00:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Timestamp when the product was last updated",
        example = "2024-01-15T14:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime updatedAt,

    @Schema(
        description = "User who created the product",
        example = "admin@company.com"
    )
    String createdBy,

    @Schema(
        description = "User who last updated the product",
        example = "admin@company.com"
    )
    String updatedBy,

    @Schema(
        description = "Version number for optimistic locking",
        example = "1"
    )
    Long version
) {

    /**
     * Convert ProductEntity to ProductResponse
     */
    public static ProductResponse from(ProductEntity product) {
        return new ProductResponse(
            product.getId(),
            product.getProductCode(),
            product.getName(),
            product.getDescription(),
            product.getProductType().name(),
            product.getProductType().getDisplayName(),
            product.getCategory() != null ? product.getCategory().name() : null,
            product.getCategory() != null ? product.getCategory().getDisplayName() : null,
            product.getPrice(),
            product.getCurrency(),
            product.getBillingPeriod(),
            product.getStatus().name(),
            product.getStatus().getDisplayName(),
            product.getValidityStart(),
            product.getValidityEnd(),
            product.isActive(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getCreatedBy(),
            product.getUpdatedBy(),
            product.getVersion()
        );
    }
}
