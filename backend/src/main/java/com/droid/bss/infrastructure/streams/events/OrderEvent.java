package com.droid.bss.infrastructure.streams.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order event for Kafka Streams
 */
public class OrderEvent {
    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private BigDecimal totalAmount;
    private Integer itemsCount;
    private String status;
    private String region;
    private List<String> itemCategories;
    private Instant timestamp;

    // Getters and setters
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getItemsCount() { return itemsCount; }
    public void setItemsCount(Integer itemsCount) { this.itemsCount = itemsCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public List<String> getItemCategories() { return itemCategories; }
    public void setItemCategories(List<String> itemCategories) { this.itemCategories = itemCategories; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    // Builder
    public static class Builder {
        private OrderEvent event = new OrderEvent();

        public Builder orderId(UUID orderId) {
            event.orderId = orderId;
            return this;
        }

        public Builder customerId(UUID customerId) {
            event.customerId = customerId;
            return this;
        }

        public Builder productId(UUID productId) {
            event.productId = productId;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            event.totalAmount = totalAmount;
            return this;
        }

        public Builder itemsCount(Integer itemsCount) {
            event.itemsCount = itemsCount;
            return this;
        }

        public Builder status(String status) {
            event.status = status;
            return this;
        }

        public Builder region(String region) {
            event.region = region;
            return this;
        }

        public Builder itemCategories(List<String> itemCategories) {
            event.itemCategories = itemCategories;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            event.timestamp = timestamp;
            return this;
        }

        public OrderEvent build() {
            return event;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
