package com.droid.bss.domain.order;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.CustomerEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity for customer order management
 */
@Entity
@Table(name = "orders")
@SQLRestriction("deleted_at IS NULL")
public class OrderEntity extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priority", length = 10)
    private OrderPriority priority = OrderPriority.NORMAL;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Column(name = "promised_date")
    private LocalDate promisedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "order_channel", length = 50)
    private String orderChannel;

    @Column(name = "sales_rep_id", length = 100)
    private String salesRepId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    // Constructors
    public OrderEntity() {}

    public OrderEntity(
            String orderNumber,
            CustomerEntity customer,
            OrderType orderType,
            OrderStatus status,
            OrderPriority priority,
            BigDecimal totalAmount,
            String currency,
            LocalDate requestedDate,
            String orderChannel,
            String salesRepId
    ) {
        this.orderNumber = orderNumber;
        this.customer = customer;
        this.orderType = orderType;
        this.status = status;
        this.priority = priority;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.requestedDate = requestedDate;
        this.orderChannel = orderChannel;
        this.salesRepId = salesRepId;
    }

    // Business methods
    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING || status == OrderStatus.APPROVED;
    }

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    public void removeItem(OrderItemEntity item) {
        items.remove(item);
        item.setOrder(null);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItemEntity::getTotalPrice)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.completedDate = LocalDate.now();
    }

    public void approve() {
        if (status == OrderStatus.PENDING || status == OrderStatus.DRAFT) {
            this.status = OrderStatus.APPROVED;
        } else {
            throw new IllegalStateException("Order cannot be approved from status: " + status);
        }
    }

    public void reject(String reason) {
        if (status == OrderStatus.PENDING || status == OrderStatus.DRAFT) {
            this.status = OrderStatus.REJECTED;
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Rejected: " + reason;
        } else {
            throw new IllegalStateException("Order cannot be rejected from status: " + status);
        }
    }

    public void startProcessing() {
        if (status == OrderStatus.APPROVED || status == OrderStatus.PENDING) {
            this.status = OrderStatus.PROCESSING;
        } else {
            throw new IllegalStateException("Order cannot start processing from status: " + status);
        }
    }

    public void markAsInProgress() {
        if (status == OrderStatus.PROCESSING) {
            this.status = OrderStatus.IN_PROGRESS;
        } else {
            throw new IllegalStateException("Order cannot be marked in progress from status: " + status);
        }
    }

    public void cancel(String reason) {
        if (canBeCancelled()) {
            this.status = OrderStatus.CANCELLED;
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Cancelled: " + reason;
        } else {
            throw new IllegalStateException("Order cannot be cancelled from status: " + status);
        }
    }

    public boolean isActive() {
        return status != OrderStatus.CANCELLED && status != OrderStatus.REJECTED;
    }

    public boolean canBeApproved() {
        return status == OrderStatus.DRAFT || status == OrderStatus.PENDING;
    }

    public boolean canBeProcessed() {
        return status == OrderStatus.APPROVED || status == OrderStatus.PENDING;
    }

    // Getters and setters
    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public void setPriority(OrderPriority priority) {
        this.priority = priority;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalDate getPromisedDate() {
        return promisedDate;
    }

    public void setPromisedDate(LocalDate promisedDate) {
        this.promisedDate = promisedDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public String getOrderChannel() {
        return orderChannel;
    }

    public void setOrderChannel(String orderChannel) {
        this.orderChannel = orderChannel;
    }

    public String getSalesRepId() {
        return salesRepId;
    }

    public void setSalesRepId(String salesRepId) {
        this.salesRepId = salesRepId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDate deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<OrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<OrderItemEntity> items) {
        this.items = items;
    }

    /**
     * Converts JPA entity to DDD aggregate
     */
    public Order toDomain() {
        return Order.restore(
            this.getId(),
            this.orderNumber,
            this.customer != null ? this.customer.getId() : null,
            this.orderType,
            this.status,
            this.priority,
            this.totalAmount,
            this.currency,
            this.requestedDate,
            this.promisedDate,
            this.orderChannel,
            this.salesRepId,
            this.notes,
            this.getCreatedAt(),
            this.getUpdatedAt(),
            this.version != null ? this.version.intValue() : 0,
            this.items != null
                ? this.items.stream()
                    .map(OrderItemEntity::toDomain)
                    .toList()
                : java.util.Collections.emptyList()
        );
    }

    /**
     * Creates JPA entity from DDD aggregate
     */
    public static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId().value());
        entity.setOrderNumber(order.getOrderNumber());
        entity.setOrderType(order.getOrderType());
        entity.setStatus(order.getStatus());
        entity.setPriority(order.getPriority());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setCurrency(order.getCurrency());
        entity.setRequestedDate(order.getRequestedDate());
        entity.setPromisedDate(order.getPromisedDate());
        entity.setOrderChannel(order.getOrderChannel());
        entity.setSalesRepId(order.getSalesRepId());
        entity.setNotes(order.getNotes());
        // Note: customer relationship should be set by caller
        // Note: items relationship should be set by caller
        return entity;
    }
}
