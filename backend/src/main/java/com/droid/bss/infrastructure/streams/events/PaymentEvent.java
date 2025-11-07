package com.droid.bss.infrastructure.streams.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment event for Kafka Streams fraud detection
 */
public class PaymentEvent {
    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String location;
    private Double fraudScore;
    private Instant timestamp;

    // Getters and setters
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getFraudScore() { return fraudScore; }
    public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    // Builder
    public static class Builder {
        private PaymentEvent event = new PaymentEvent();

        public Builder paymentId(UUID paymentId) {
            event.paymentId = paymentId;
            return this;
        }

        public Builder orderId(UUID orderId) {
            event.orderId = orderId;
            return this;
        }

        public Builder customerId(UUID customerId) {
            event.customerId = customerId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            event.amount = amount;
            return this;
        }

        public Builder status(String status) {
            event.status = status;
            return this;
        }

        public Builder paymentMethod(String paymentMethod) {
            event.paymentMethod = paymentMethod;
            return this;
        }

        public Builder location(String location) {
            event.location = location;
            return this;
        }

        public Builder fraudScore(Double fraudScore) {
            event.fraudScore = fraudScore;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            event.timestamp = timestamp;
            return this;
        }

        public PaymentEvent build() {
            return event;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
