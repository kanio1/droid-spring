package com.droid.bss.domain.order;

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
 * Order aggregate root
 * Manages order lifecycle and items
 */
public class Order {

    private final OrderId id;
    private final String orderNumber;
    private final CustomerId customerId;
    private final OrderType orderType;
    private final OrderStatus status;
    private final OrderPriority priority;
    private final BigDecimal totalAmount;
    private final String currency;
    private final LocalDate requestedDate;
    private final LocalDate promisedDate;
    private final String orderChannel;
    private final String salesRepId;
    private final String notes;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int version;

    // Immutable collection of items
    private final List<OrderItem> items;

    /**
     * Package-private constructor for infrastructure layer.
     * Use factory methods for domain operations.
     */
    Order(
            OrderId id,
            String orderNumber,
            CustomerId customerId,
            OrderType orderType,
            OrderStatus status,
            OrderPriority priority,
            BigDecimal totalAmount,
            String currency,
            LocalDate requestedDate,
            LocalDate promisedDate,
            String orderChannel,
            String salesRepId,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version,
            List<OrderItem> items
    ) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.orderNumber = Objects.requireNonNull(orderNumber, "Order number cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.orderType = Objects.requireNonNull(orderType, "Order type cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.priority = priority != null ? priority : OrderPriority.NORMAL;
        this.totalAmount = totalAmount;
        this.currency = currency != null ? currency : "PLN";
        this.requestedDate = requestedDate;
        this.promisedDate = promisedDate;
        this.orderChannel = orderChannel;
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
     * Creates a new Order with single item
     */
    public static Order create(
            CustomerId customerId,
            String orderNumber,
            OrderItem item,
            OrderType orderType
    ) {
        return create(customerId, orderNumber, List.of(item), orderType, null, null);
    }

    /**
     * Creates a new Order with multiple items
     */
    public static Order create(
            CustomerId customerId,
            String orderNumber,
            List<OrderItem> items,
            OrderType orderType
    ) {
        return create(customerId, orderNumber, items, orderType, null, null);
    }

    /**
     * Creates a new Order with all parameters
     */
    public static Order create(
            CustomerId customerId,
            String orderNumber,
            List<OrderItem> items,
            OrderType orderType,
            LocalDate requestedDate,
            String orderChannel
    ) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(orderNumber, "Order number cannot be null");
        Objects.requireNonNull(items, "Items cannot be null");
        Objects.requireNonNull(orderType, "Order type cannot be null");

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        OrderId orderId = OrderId.generate();
        BigDecimal totalAmount = calculateTotal(items);
        LocalDateTime now = LocalDateTime.now();

        return new Order(
            orderId,
            orderNumber,
            customerId,
            orderType,
            OrderStatus.PENDING,
            OrderPriority.NORMAL,
            totalAmount,
            "PLN",
            requestedDate,
            null,
            orderChannel,
            null,
            null,
            now,
            now,
            1,
            items
        );
    }

    /**
     * Updates order status (immutable operation)
     */
    public Order changeStatus(OrderStatus newStatus) {
        validateStatusTransition(this.status, newStatus);

        LocalDate completedDate = (newStatus == OrderStatus.COMPLETED) ? LocalDate.now() : null;
        LocalDateTime now = LocalDateTime.now();

        return new Order(
            this.id,
            this.orderNumber,
            this.customerId,
            this.orderType,
            newStatus,
            this.priority,
            this.totalAmount,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Adds an item to the order (immutable operation)
     */
    public Order addItem(OrderItem newItem) {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot add items to order with status: " + this.status);
        }

        List<OrderItem> newItems = new ArrayList<>(this.items);
        newItems.add(newItem);
        BigDecimal newTotal = calculateTotal(newItems);

        LocalDateTime now = LocalDateTime.now();

        return new Order(
            this.id,
            this.orderNumber,
            this.customerId,
            this.orderType,
            this.status,
            this.priority,
            newTotal,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            newItems
        );
    }

    /**
     * Updates an item in the order (immutable operation)
     */
    public Order updateItem(UUID itemId, OrderItem updatedItem) {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot update items in order with status: " + this.status);
        }

        List<OrderItem> newItems = new ArrayList<>();
        boolean found = false;

        for (OrderItem item : this.items) {
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

        return new Order(
            this.id,
            this.orderNumber,
            this.customerId,
            this.orderType,
            this.status,
            this.priority,
            newTotal,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            newItems
        );
    }

    /**
     * Removes an item from the order (immutable operation)
     */
    public Order removeItem(UUID itemId) {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.DRAFT) {
            throw new IllegalArgumentException("Cannot remove items from order with status: " + this.status);
        }

        List<OrderItem> newItems = new ArrayList<>();
        boolean found = false;

        for (OrderItem item : this.items) {
            if (!item.getId().equals(itemId)) {
                newItems.add(item);
            } else {
                found = true;
            }
        }

        if (!found) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        if (newItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        BigDecimal newTotal = calculateTotal(newItems);
        LocalDateTime now = LocalDateTime.now();

        return new Order(
            this.id,
            this.orderNumber,
            this.customerId,
            this.orderType,
            this.status,
            this.priority,
            newTotal,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            this.notes,
            this.createdAt,
            now,
            this.version + 1,
            newItems
        );
    }

    /**
     * Cancels the order (immutable operation)
     */
    public Order cancel(String reason) {
        if (!canBeCancelled()) {
            throw new IllegalArgumentException("Order with status " + this.status + " cannot be cancelled");
        }

        LocalDateTime now = LocalDateTime.now();

        return new Order(
            this.id,
            this.orderNumber,
            this.customerId,
            this.orderType,
            OrderStatus.CANCELLED,
            this.priority,
            this.totalAmount,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            reason,
            this.createdAt,
            now,
            this.version + 1,
            this.items
        );
    }

    /**
     * Calculates total amount from items
     */
    private static BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates status transition
     */
    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Define valid transitions
        switch (from) {
            case DRAFT:
                if (to != OrderStatus.PENDING && to != OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from DRAFT to " + to);
                }
                break;
            case PENDING:
                if (to != OrderStatus.APPROVED && to != OrderStatus.REJECTED && to != OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from PENDING to " + to);
                }
                break;
            case APPROVED:
                if (to != OrderStatus.IN_PROGRESS && to != OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from APPROVED to " + to);
                }
                break;
            case IN_PROGRESS:
                if (to != OrderStatus.PROCESSING && to != OrderStatus.CANCELLED) {
                    throw new IllegalArgumentException("Invalid transition from IN_PROGRESS to " + to);
                }
                break;
            case PROCESSING:
                if (to != OrderStatus.COMPLETED) {
                    throw new IllegalArgumentException("Invalid transition from PROCESSING to " + to);
                }
                break;
            case REJECTED:
            case CANCELLED:
            case COMPLETED:
                throw new IllegalArgumentException("Cannot transition from " + from);
        }
    }

    // Business methods
    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING ||
               status == OrderStatus.APPROVED ||
               status == OrderStatus.IN_PROGRESS;
    }

    public boolean canBeModified() {
        return status == OrderStatus.PENDING || status == OrderStatus.DRAFT;
    }

    // Getters
    public OrderId getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public LocalDate getPromisedDate() {
        return promisedDate;
    }

    public String getOrderChannel() {
        return orderChannel;
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

    public List<OrderItem> getItems() {
        return items;
    }

    /**
     * Restores Order from persistence state (for infrastructure layer)
     * Public - use by repository implementations only
     */
    public static Order restore(
            UUID id,
            String orderNumber,
            UUID customerId,
            OrderType orderType,
            OrderStatus status,
            OrderPriority priority,
            BigDecimal totalAmount,
            String currency,
            LocalDate requestedDate,
            LocalDate promisedDate,
            String orderChannel,
            String salesRepId,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version,
            List<OrderItem> items
    ) {
        return new Order(
            new OrderId(id),
            orderNumber,
            new CustomerId(customerId),
            orderType,
            status,
            priority,
            totalAmount,
            currency,
            requestedDate,
            promisedDate,
            orderChannel,
            salesRepId,
            notes,
            createdAt,
            updatedAt,
            version,
            items
        );
    }
}
