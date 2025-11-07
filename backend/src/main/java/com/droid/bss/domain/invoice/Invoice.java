package com.droid.bss.domain.invoice;

import com.droid.bss.domain.customer.CustomerId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice aggregate root
 * Manages invoice lifecycle and items
 */
public class Invoice {

    private final InvoiceId id;
    private final String invoiceNumber;
    private final CustomerId customerId;
    private final InvoiceStatus status;
    private final BigDecimal totalAmount;
    private final String currency;
    private final LocalDate invoiceDate;
    private final LocalDate dueDate;
    private final String orderNumber;
    private final String salesRepId;
    private final String notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    // Immutable collection of items
    private final List<InvoiceItem> items;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Invoice(
            InvoiceId id,
            String invoiceNumber,
            CustomerId customerId,
            InvoiceStatus status,
            BigDecimal totalAmount,
            String currency,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String orderNumber,
            String salesRepId,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version,
            List<InvoiceItem> items
    ) {
        this.id = Objects.requireNonNull(id, "Invoice ID cannot be null");
        this.invoiceNumber = Objects.requireNonNull(invoiceNumber, "Invoice number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.totalAmount = totalAmount;
        this.currency = currency != null ? currency : "PLN";
        this.invoiceDate = invoiceDate != null ? invoiceDate : LocalDate.now();
        this.dueDate = dueDate;
        this.orderNumber = orderNumber;
        this.salesRepId = salesRepId;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(createdAt, "Created date cannot be null");
        this.updatedAt = this.createdAt;
        this.version = version;
        this.items = Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"))
        );
    }

    /**
     * Creates a new Invoice with single item
     */
    public static Invoice create(
            String invoiceNumber,
            CustomerId customerId,
            InvoiceItem item,
            String orderNumber
    ) {
        return create(invoiceNumber, customerId, List.of(item), orderNumber, null, null);
    }

    /**
     * Creates a new Invoice with multiple items
     */
    public static Invoice create(
            String invoiceNumber,
            CustomerId customerId,
            List<InvoiceItem> items,
            String orderNumber
    ) {
        return create(invoiceNumber, customerId, items, orderNumber, null, null);
    }

    /**
     * Creates a new Invoice with all parameters
     */
    public static Invoice create(
            String invoiceNumber,
            CustomerId customerId,
            List<InvoiceItem> items,
            String orderNumber,
            LocalDate dueDate,
            String salesRepId
    ) {
        Objects.requireNonNull(invoiceNumber, "Invoice number cannot be null");
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(items, "Items cannot be null");
        Objects.requireNonNull(orderNumber, "Order number cannot be null");

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Invoice must have at least one item");
        }

        InvoiceId invoiceId = InvoiceId.generate();
        BigDecimal totalAmount = calculateTotal(items);
        LocalDateTime now = LocalDateTime.now();
        LocalDate invoiceDate = LocalDate.now();
        LocalDate computedDueDate = dueDate != null ? dueDate : invoiceDate.plusDays(30);

        return new Invoice(
            invoiceId,
            invoiceNumber,
            customerId,
            InvoiceStatus.DRAFT,
            totalAmount,
            "PLN",
            invoiceDate,
            computedDueDate,
            orderNumber,
            salesRepId,
            null,
            now,
            now,
            1,
            items
        );
    }

    /**
     * Updates invoice status (immutable operation)
     */
    public Invoice changeStatus(InvoiceStatus newStatus) {
        validateStatusTransition(this.status, newStatus);

        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            newStatus,
            this.totalAmount,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Sends invoice (immutable operation)
     */
    public Invoice send() {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft invoices can be sent");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            InvoiceStatus.SENT,
            this.totalAmount,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Marks invoice as paid (immutable operation)
     */
    public Invoice markAsPaid() {
        if (this.status != InvoiceStatus.SENT && this.status != InvoiceStatus.VIEWED) {
            throw new IllegalArgumentException("Only sent or viewed invoices can be marked as paid");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            InvoiceStatus.PAID,
            this.totalAmount,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Cancels invoice (immutable operation)
     */
    public Invoice cancel(String reason) {
        if (!canBeCancelled()) {
            throw new IllegalArgumentException("Invoice with status " + this.status + " cannot be cancelled");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            InvoiceStatus.CANCELLED,
            this.totalAmount,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            reason,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Adds an item to the invoice (immutable operation)
     */
    public Invoice addItem(InvoiceItem newItem) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot add items to invoice with status: " + this.status);
        }

        List<InvoiceItem> newItems = new ArrayList<>(this.items);
        newItems.add(newItem);
        BigDecimal newTotal = calculateTotal(newItems);

        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            this.status,
            newTotal,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            newItems
        );
    }

    /**
     * Updates an item in the invoice (immutable operation)
     */
    public Invoice updateItem(UUID itemId, InvoiceItem updatedItem) {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot update items in invoice with status: " + this.status);
        }

        List<InvoiceItem> newItems = new ArrayList<>();
        boolean found = false;

        for (InvoiceItem item : this.items) {
            if (item.getId().equals(itemId)) {
                newItems.add(updatedItem);
                found = true;
            } else {
                newItems.add(item);
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        BigDecimal newTotal = calculateTotal(newItems);
        LocalDateTime now = LocalDateTime.now();

        return new Invoice(
            this.id,
            this.invoiceNumber,
            this.customerId,
            this.status,
            newTotal,
            this.currency,
            this.invoiceDate,
            this.dueDate,
            this.orderNumber,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            newItems
        );
    }

    /**
     * Calculates total amount from items
     */
    private static BigDecimal calculateTotal(List<InvoiceItem> items) {
        return items.stream()
            .map(InvoiceItem::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates status transition
     */
    private void validateStatusTransition(InvoiceStatus from, InvoiceStatus to) {
        switch (from) {
            case DRAFT:
                if (to != InvoiceStatus.SENT && to != InvoiceStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from DRAFT to " + to);
                }
                break;
            case SENT:
                if (to != InvoiceStatus.VIEWED && to != InvoiceStatus.PAID && to != InvoiceStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from SENT to " + to);
                }
                break;
            case VIEWED:
                if (to != InvoiceStatus.PAID && to != InvoiceStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from VIEWED to " + to);
                }
                break;
            case PAID:
            case CANCELLED:
            case OVERDUE:
                throw new IllegalArgumentException("Cannot transition from " + from);
        }
    }

    // Business methods
    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean isSent() {
        return status == InvoiceStatus.SENT;
    }

    public boolean isPaid() {
        return status == InvoiceStatus.PAID;
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

    public boolean canBeModified() {
        return status == InvoiceStatus.DRAFT;
    }

    public boolean canBeCancelled() {
        return status == InvoiceStatus.DRAFT || status == InvoiceStatus.SENT;
    }

    public boolean isOverdue() {
        return status == InvoiceStatus.OVERDUE ||
               (status == InvoiceStatus.SENT && LocalDate.now().isAfter(dueDate));
    }

    // Getters
    public InvoiceId getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getSalesRepId() {
        return salesRepId;
    }

    public String getNotes() {
        return notes;
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

    public List<InvoiceItem> getItems() {
        return items;
    }

    /**
     * Restores Invoice from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static Invoice restore(
            UUID id,
            String invoiceNumber,
            UUID customerId,
            InvoiceStatus status,
            BigDecimal totalAmount,
            String currency,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String orderNumber,
            String salesRepId,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version,
            List<InvoiceItem> items
    ) {
        return new Invoice(
            new InvoiceId(id),
            invoiceNumber,
            new CustomerId(customerId),
            status,
            totalAmount,
            currency,
            invoiceDate,
            dueDate,
            orderNumber,
            salesRepId,
            notes,
            createdAt,
            updatedAt,
            version,
            items
        );
    }
}
