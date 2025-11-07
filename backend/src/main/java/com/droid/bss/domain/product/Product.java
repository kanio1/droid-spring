package com.droid.bss.domain.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Product aggregate root
 * Represents services, tariffs, bundles, and add-ons
 */
public class Product {

    private final ProductId id;
    private final String productCode;
    private final String name;
    private final String description;
    private final ProductType productType;
    private final ProductCategory category;
    private final BigDecimal price;
    private final String currency;
    private final String billingPeriod;
    private final ProductStatus status;
    private final LocalDate validityStart;
    private final LocalDate validityEnd;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Product(
            ProductId id,
            String productCode,
            String name,
            String description,
            ProductType productType,
            ProductCategory category,
            BigDecimal price,
            String currency,
            String billingPeriod,
            ProductStatus status,
            LocalDate validityStart,
            LocalDate validityEnd,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "Product ID cannot be null");
        this.productCode = Objects.requireNonNull(productCode, "Product code cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = description;
        this.productType = Objects.requireNonNull(productType, "Product type cannot be null");
        this.category = category;
        this.price = Objects.requireNonNull(price, "Price cannot be null");
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.currency = currency != null ? currency : "PLN";
        this.billingPeriod = Objects.requireNonNull(billingPeriod, "Billing period cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.validityStart = validityStart;
        this.validityEnd = validityEnd;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = this.createdAt;
        this.version = version;

        // Validate date range
        if (validityStart != null && validityEnd != null && validityStart.isAfter(validityEnd)) {
            throw new IllegalArgumentException("Validity start date cannot be after validity end date");
        }
    }

    /**
     * Creates a new Product
     */
    public static Product create(
            String productCode,
            String name,
            String description,
            ProductType productType,
            ProductCategory category,
            BigDecimal price,
            String billingPeriod
    ) {
        return create(
            productCode,
            name,
            description,
            productType,
            category,
            price,
            "PLN",
            billingPeriod,
            ProductStatus.ACTIVE,
            null,
            null
        );
    }

    /**
     * Creates a new Product with all parameters
     */
    public static Product create(
            String productCode,
            String name,
            String description,
            ProductType productType,
            ProductCategory category,
            BigDecimal price,
            String currency,
            String billingPeriod,
            ProductStatus status,
            LocalDate validityStart,
            LocalDate validityEnd
    ) {
        Objects.requireNonNull(productCode, "Product code cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(productType, "Product type cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(billingPeriod, "Billing period cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        if (validityStart != null && validityEnd != null && validityStart.isAfter(validityEnd)) {
            throw new IllegalArgumentException("Validity start date cannot be after validity end date");
        }

        ProductId productId = ProductId.generate();
        LocalDateTime now = LocalDateTime.now();

        return new Product(
            productId,
            productCode,
            name,
            description,
            productType,
            category,
            price,
            currency,
            billingPeriod,
            status,
            validityStart,
            validityEnd,
            now,
            now,
            1
        );
    }

    /**
     * Updates product information (immutable operation)
     */
    public Product updateInfo(
            String name,
            String description,
            ProductCategory category
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new Product(
            this.id,
            this.productCode,
            Objects.requireNonNull(name, "Name cannot be null"),
            description,
            this.productType,
            category,
            this.price,
            this.currency,
            this.billingPeriod,
            this.status,
            this.validityStart,
            this.validityEnd,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Updates price (immutable operation)
     */
    public Product updatePrice(BigDecimal newPrice) {
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Product(
            this.id,
            this.productCode,
            this.name,
            this.description,
            this.productType,
            this.category,
            newPrice,
            this.currency,
            this.billingPeriod,
            this.status,
            this.validityStart,
            this.validityEnd,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Changes product status (immutable operation)
     */
    public Product changeStatus(ProductStatus newStatus) {
        LocalDateTime now = LocalDateTime.now();

        return new Product(
            this.id,
            this.productCode,
            this.name,
            this.description,
            this.productType,
            this.category,
            this.price,
            this.currency,
            this.billingPeriod,
            Objects.requireNonNull(newStatus, "Status cannot be null"),
            this.validityStart,
            this.validityEnd,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    /**
     * Updates validity period (immutable operation)
     */
    public Product updateValidity(LocalDate validityStart, LocalDate validityEnd) {
        if (validityStart != null && validityEnd != null && validityStart.isAfter(validityEnd)) {
            throw new IllegalArgumentException("Validity start date cannot be after validity end date");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Product(
            this.id,
            this.productCode,
            this.name,
            this.description,
            this.productType,
            this.category,
            this.price,
            this.currency,
            this.billingPeriod,
            this.status,
            validityStart,
            validityEnd,
            this.createdAt,
            now,
            this.version + 1
        );
    }

    // Business methods
    public boolean isActive() {
        return status == ProductStatus.ACTIVE;
    }

    public boolean isInactive() {
        return status == ProductStatus.INACTIVE;
    }

    public boolean isDiscontinued() {
        return status == ProductStatus.DISCONTINUED;
    }

    public boolean isAvailable() {
        return isActive() && isWithinValidityPeriod();
    }

    public boolean isWithinValidityPeriod() {
        LocalDate now = LocalDate.now();
        if (validityStart != null && now.isBefore(validityStart)) {
            return false;
        }
        if (validityEnd != null && now.isAfter(validityEnd)) {
            return false;
        }
        return true;
    }

    public boolean canBeModified() {
        return isActive();
    }

    public boolean canBeOrdered() {
        return isAvailable() && productType != ProductType.ADDON;
    }

    // Getters
    public ProductId getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductType getProductType() {
        return productType;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public LocalDate getValidityStart() {
        return validityStart;
    }

    public LocalDate getValidityEnd() {
        return validityEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Restores Product from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static Product restore(
            UUID id,
            String productCode,
            String name,
            String description,
            ProductType productType,
            ProductCategory category,
            BigDecimal price,
            String currency,
            String billingPeriod,
            ProductStatus status,
            LocalDate validityStart,
            LocalDate validityEnd,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version
    ) {
        return new Product(
            new ProductId(id),
            productCode,
            name,
            description,
            productType,
            category,
            price,
            currency,
            billingPeriod,
            status,
            validityStart,
            validityEnd,
            createdAt,
            updatedAt,
            version
        );
    }
}
