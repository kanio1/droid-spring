package com.droid.bss.api.graphql;

import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.infrastructure.read.OrderReadRepository;
import com.droid.bss.infrastructure.read.CustomerReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for Order-related queries and mutations
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OrderGraphQLController {

    private final OrderReadRepository orderRepository;
    private final CustomerReadRepository customerRepository;

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<OrderEntity> order(@Argument UUID id) {
        log.debug("Fetching order with id: {}", id);
        return CompletableFuture.supplyAsync(() ->
            orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id))
        );
    }

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<List<OrderEntity>> orders(
            @Argument Optional<Integer> page,
            @Argument Optional<Integer> size,
            @Argument Optional<OrderStatus> status,
            @Argument Optional<UUID> customerId) {

        log.debug("Fetching orders with filters");
        return CompletableFuture.supplyAsync(() -> {
            List<OrderEntity> orders = orderRepository.findAll();

            if (status.isPresent()) {
                orders = orders.stream()
                    .filter(o -> o.getStatus() == status.get())
                    .collect(java.util.stream.Collectors.toList());
            }

            if (customerId.isPresent()) {
                orders = orders.stream()
                    .filter(o -> o.getCustomer().getId().equals(customerId.get()))
                    .collect(java.util.stream.Collectors.toList());
            }

            return orders;
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<OrderEntity> createOrder(@Argument("input") CreateOrderInput input) {
        log.info("Creating order for customer: {}", input.getCustomerId());
        return CompletableFuture.supplyAsync(() -> {
            CustomerEntity customer = customerRepository.findById(input.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + input.getCustomerId()));

            BigDecimal totalAmount = calculateOrderTotal(input.getItems());
            BigDecimal taxAmount = calculateTax(totalAmount);
            BigDecimal shippingAmount = input.getShippingAmount() != null ? input.getShippingAmount() : BigDecimal.ZERO;
            BigDecimal discountAmount = input.getDiscountAmount() != null ? input.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal finalTotal = totalAmount.add(taxAmount).add(shippingAmount).subtract(discountAmount);

            OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(discountAmount)
                .currency(input.getCurrency() != null ? input.getCurrency() : "PLN")
                .shippingAddress(input.getShippingAddress())
                .billingAddress(input.getBillingAddress())
                .notes(input.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            return orderRepository.save(order);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<OrderEntity> updateOrder(
            @Argument UUID id,
            @Argument("input") UpdateOrderInput input) {

        log.info("Updating order: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

            if (input.getStatus() != null) order.setStatus(input.getStatus());
            if (input.getShippingAddress() != null) order.setShippingAddress(input.getShippingAddress());
            if (input.getBillingAddress() != null) order.setBillingAddress(input.getBillingAddress());
            if (input.getNotes() != null) order.setNotes(input.getNotes());
            if (input.getShippedAt() != null) order.setShippedAt(input.getShippedAt());
            if (input.getDeliveredAt() != null) order.setDeliveredAt(input.getDeliveredAt());
            order.setUpdatedAt(LocalDateTime.now());

            return orderRepository.save(order);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<OrderEntity> cancelOrder(
            @Argument UUID id,
            @Argument Optional<String> reason) {

        log.info("Cancelling order: {}", id);
        return CompletableFuture.supplyAsync(() -> {
            OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

            order.setStatus(OrderStatus.CANCELLED);
            order.setNotes((order.getNotes() != null ? order.getNotes() : "") +
                "\nCancellation reason: " + reason.orElse("No reason provided"));
            order.setUpdatedAt(LocalDateTime.now());

            return orderRepository.save(order);
        });
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompletableFuture<OrderEntity> changeOrderStatus(
            @Argument UUID id,
            @Argument OrderStatus status) {

        log.info("Changing order status to: {} for order: {}", status, id);
        return CompletableFuture.supplyAsync(() -> {
            OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());

            // Set timestamps based on status
            if (status == OrderStatus.SHIPPED && order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
            } else if (status == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
                order.setDeliveredAt(LocalDateTime.now());
            }

            return orderRepository.save(order);
        });
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private BigDecimal calculateOrderTotal(List<OrderItemInput> items) {
        return items.stream()
            .map(item -> {
                BigDecimal unitPrice = item.getUnitPrice() != null ?
                    item.getUnitPrice() : BigDecimal.ZERO;
                return unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTax(BigDecimal amount) {
        // 23% VAT
        return amount.multiply(new BigDecimal("0.23"));
    }

    // ========== INPUT CLASSES ==========

    public static class CreateOrderInput {
        private UUID customerId;
        private String currency;
        private String shippingAddress;
        private String billingAddress;
        private String notes;
        private BigDecimal shippingAmount;
        private BigDecimal discountAmount;
        private List<OrderItemInput> items;

        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

        public String getBillingAddress() { return billingAddress; }
        public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public BigDecimal getShippingAmount() { return shippingAmount; }
        public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }

        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

        public List<OrderItemInput> getItems() { return items; }
        public void setItems(List<OrderItemInput> items) { this.items = items; }
    }

    public static class OrderItemInput {
        private UUID productId;
        private Integer quantity;
        private BigDecimal unitPrice;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }

    public static class UpdateOrderInput {
        private OrderStatus status;
        private String shippingAddress;
        private String billingAddress;
        private String notes;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;

        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }

        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

        public String getBillingAddress() { return billingAddress; }
        public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public LocalDateTime getShippedAt() { return shippedAt; }
        public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

        public LocalDateTime getDeliveredAt() { return deliveredAt; }
        public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    }
}
