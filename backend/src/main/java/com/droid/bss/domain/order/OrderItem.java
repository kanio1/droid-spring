package com.droid.bss.domain.order;

import com.droid.bss.domain.product.ProductId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * OrderItem entity within Order aggregate
 * Manages individual items in an order
 */
public class OrderItem {

    private final UUID id;
    private final ProductId productId;
    private final OrderItemType itemType;
    private final String itemCode;
    private final String itemName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountAmount;
    private final BigDecimal taxRate;
    private final OrderItemStatus status;
    private final LocalDate activationDate;
    private final LocalDate expiryDate;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    OrderItem(
            UUID id,
            ProductId productId,
            OrderItemType itemType,
            String itemCode,
            String itemName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate,
            OrderItemStatus status,
            LocalDate activationDate,
            LocalDate expiryDate,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "OrderItem ID cannot be null");
        this.productId = Objects.requireNonNull(productId, "ProductId cannot be null");
        this.itemType = Objects.requireNonNull(itemType, "Item type cannot be null");
        this.itemCode = itemCode;
        this.itemName = Objects.requireNonNull(itemName, "Item name cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.taxRate = Objects.requireNonNull(taxRate, "Tax rate cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.activationDate = activationDate;
        this.expiryDate = expiryDate;
        this.version = version;
    }

    /**
     * Creates a new OrderItem
     */
    public static OrderItem create(
            ProductId productId,
            OrderItemType itemType,
            String itemCode,
            String itemName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate
    ) {
        return new OrderItem(
            UUID.randomUUID(),
            productId,
            itemType,
            itemCode,
            itemName,
            quantity,
            unitPrice,
            discountAmount,
            taxRate,
            OrderItemStatus.PENDING,
            null,
            null,
            1
        );
    }

    /**
     * Creates a new OrderItem with default tax rate
     */
    public static OrderItem create(
            ProductId productId,
            String itemName,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        return create(
            productId,
            OrderItemType.PRODUCT,
            null,
            itemName,
            quantity,
            unitPrice,
            BigDecimal.ZERO,
            BigDecimal.valueOf(23.00)
        );
    }

    /**
     * Updates quantity (immutable operation)
     */
    public OrderItem updateQuantity(Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.status != OrderItemStatus.PENDING) {
            throw new IllegalArgumentException("Cannot update quantity of non-pending item");
        }
        return new OrderItem(
            this.id,
            this.productId,
            this.itemType,
            this.itemCode,
            this.itemName,
            newQuantity,
            this.unitPrice,
            this.discountAmount,
            this.taxRate,
            this.status,
            this.activationDate,
            this.expiryDate,
            this.version + 1
        );
    }

    /**
     * Updates unit price (immutable operation)
     */
    public OrderItem updateUnitPrice(BigDecimal newUnitPrice) {
        if (newUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        if (this.status != OrderItemStatus.PENDING) {
            throw new IllegalArgumentException("Cannot update price of non-pending item");
        }
        return new OrderItem(
            this.id,
            this.productId,
            this.itemType,
            this.itemCode,
            this.itemName,
            this.quantity,
            newUnitPrice,
            this.discountAmount,
            this.taxRate,
            this.status,
            this.activationDate,
            this.expiryDate,
            this.version + 1
        );
    }

    /**
     * Changes status (immutable operation)
     */
    public OrderItem changeStatus(OrderItemStatus newStatus) {
        return new OrderItem(
            this.id,
            this.productId,
            this.itemType,
            this.itemCode,
            this.itemName,
            this.quantity,
            this.unitPrice,
            this.discountAmount,
            this.taxRate,
            newStatus,
            newStatus == OrderItemStatus.ACTIVE ? LocalDate.now() : this.activationDate,
            this.expiryDate,
            this.version + 1
        );
    }

    /**
     * Calculates total price before discount
     */
    public BigDecimal getTotalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Calculates net amount (after discount)
     */
    public BigDecimal getNetAmount() {
        return getTotalPrice().subtract(discountAmount);
    }

    /**
     * Calculates tax amount
     */
    public BigDecimal getTaxAmount() {
        return getNetAmount().multiply(taxRate).divide(BigDecimal.valueOf(100));
    }

    /**
     * Calculates final amount (with tax)
     */
    public BigDecimal getFinalAmount() {
        return getNetAmount().add(getTaxAmount());
    }

    /**
     * Checks if item can be activated
     */
    public boolean canBeActivated() {
        return status == OrderItemStatus.PENDING;
    }

    /**
     * Checks if item is active
     */
    public boolean isActive() {
        return status == OrderItemStatus.ACTIVE;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public OrderItemType getItemType() {
        return itemType;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Restores OrderItem from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static OrderItem restore(
            UUID id,
            UUID productId,
            OrderItemType itemType,
            String itemCode,
            String itemName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate,
            OrderItemStatus status,
            LocalDate activationDate,
            LocalDate expiryDate,
            int version
    ) {
        return new OrderItem(
            id,
            new ProductId(productId),
            itemType,
            itemCode,
            itemName,
            quantity,
            unitPrice,
            discountAmount,
            taxRate,
            status,
            activationDate,
            expiryDate,
            version
        );
    }
}
