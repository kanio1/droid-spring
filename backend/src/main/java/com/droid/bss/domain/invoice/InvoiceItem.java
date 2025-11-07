package com.droid.bss.domain.invoice;

import com.droid.bss.domain.order.OrderId;
import com.droid.bss.domain.product.ProductId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * InvoiceItem entity within Invoice aggregate
 * Manages individual items in an invoice
 */
public class InvoiceItem {

    private final UUID id;
    private final OrderId orderId;
    private final ProductId productId;
    private final String description;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountAmount;
    private final BigDecimal taxRate;
    private final InvoiceItemStatus status;
    private final int version;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    InvoiceItem(
            UUID id,
            OrderId orderId,
            ProductId productId,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate,
            InvoiceItemStatus status,
            int version
    ) {
        this.id = Objects.requireNonNull(id, "InvoiceItem ID cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.productId = productId;
        this.description = Objects.requireNonNull(description, "Description cannot be null");
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
        this.version = version;
    }

    /**
     * Creates a new InvoiceItem
     */
    public static InvoiceItem create(
            OrderId orderId,
            ProductId productId,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate
    ) {
        return new InvoiceItem(
            UUID.randomUUID(),
            orderId,
            productId,
            description,
            quantity,
            unitPrice,
            discountAmount,
            taxRate,
            InvoiceItemStatus.PENDING,
            1
        );
    }

    /**
     * Creates a new InvoiceItem with default tax rate
     */
    public static InvoiceItem create(
            OrderId orderId,
            String description,
            Integer quantity,
            BigDecimal unitPrice
    ) {
        return create(
            orderId,
            null,
            description,
            quantity,
            unitPrice,
            BigDecimal.ZERO,
            BigDecimal.valueOf(23.00)
        );
    }

    /**
     * Updates quantity (immutable operation)
     */
    public InvoiceItem updateQuantity(Integer newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new InvoiceItem(
            this.id,
            this.orderId,
            this.productId,
            this.description,
            newQuantity,
            this.unitPrice,
            this.discountAmount,
            this.taxRate,
            this.status,
            this.version + 1
        );
    }

    /**
     * Updates unit price (immutable operation)
     */
    public InvoiceItem updateUnitPrice(BigDecimal newUnitPrice) {
        if (newUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        return new InvoiceItem(
            this.id,
            this.orderId,
            this.productId,
            this.description,
            this.quantity,
            newUnitPrice,
            this.discountAmount,
            this.taxRate,
            this.status,
            this.version + 1
        );
    }

    /**
     * Changes status (immutable operation)
     */
    public InvoiceItem changeStatus(InvoiceItemStatus newStatus) {
        return new InvoiceItem(
            this.id,
            this.orderId,
            this.productId,
            this.description,
            this.quantity,
            this.unitPrice,
            this.discountAmount,
            this.taxRate,
            newStatus,
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
     * Checks if item is paid
     */
    public boolean isPaid() {
        return status == InvoiceItemStatus.PAID;
    }

    /**
     * Checks if item is pending
     */
    public boolean isPending() {
        return status == InvoiceItemStatus.PENDING;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getDescription() {
        return description;
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

    public InvoiceItemStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Restores InvoiceItem from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static InvoiceItem restore(
            UUID id,
            UUID orderId,
            UUID productId,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            BigDecimal taxRate,
            InvoiceItemStatus status,
            int version
    ) {
        return new InvoiceItem(
            id,
            new OrderId(orderId),
            productId != null ? new ProductId(productId) : null,
            description,
            quantity,
            unitPrice,
            discountAmount,
            taxRate,
            status,
            version
        );
    }
}
