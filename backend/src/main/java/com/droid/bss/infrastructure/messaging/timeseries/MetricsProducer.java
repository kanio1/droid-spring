package com.droid.bss.infrastructure.messaging.timeseries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.events.ListenerContainerIdleEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class MetricsProducer {

    private static final Logger log = LoggerFactory.getLogger(MetricsProducer.class);

    private static final String CUSTOMER_METRICS_TOPIC = "bss.customer.metrics";
    private static final String ORDER_METRICS_TOPIC = "bss.order.metrics";
    private static final String PAYMENT_METRICS_TOPIC = "bss.payment.metrics";
    private static final String REVENUE_METRICS_TOPIC = "bss.revenue.metrics";
    private static final String SYSTEM_METRICS_TOPIC = "bss.system.metrics";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MetricsProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void recordCustomerMetric(UUID customerId, String metricName, Double metricValue) {
        CustomerMetricEvent event = CustomerMetricEvent.builder()
            .customerId(customerId)
            .metricName(metricName)
            .metricValue(metricValue)
            .timestamp(Instant.now())
            .build();

        sendEvent(CUSTOMER_METRICS_TOPIC, customerId.toString(), event);
    }

    public void recordCustomerMetric(UUID customerId, String metricName, Double metricValue,
                                    Map<String, Object> labels) {
        CustomerMetricEvent event = CustomerMetricEvent.builder()
            .customerId(customerId)
            .metricName(metricName)
            .metricValue(metricValue)
            .timestamp(Instant.now())
            .labels(labels)
            .build();

        sendEvent(CUSTOMER_METRICS_TOPIC, customerId.toString(), event);
    }

    public void recordOrderMetric(UUID orderId, UUID customerId, String status, BigDecimal totalAmount) {
        OrderMetricEvent event = OrderMetricEvent.builder()
            .orderId(orderId)
            .customerId(customerId)
            .status(status)
            .totalAmount(totalAmount)
            .timestamp(Instant.now())
            .build();

        sendEvent(ORDER_METRICS_TOPIC, orderId.toString(), event);
    }

    public void recordOrderMetric(UUID orderId, UUID customerId, String status,
                                 BigDecimal totalAmount, Integer itemsCount, String region) {
        OrderMetricEvent event = OrderMetricEvent.builder()
            .orderId(orderId)
            .customerId(customerId)
            .status(status)
            .totalAmount(totalAmount)
            .itemsCount(itemsCount)
            .region(region)
            .timestamp(Instant.now())
            .build();

        sendEvent(ORDER_METRICS_TOPIC, orderId.toString(), event);
    }

    public void recordPaymentMetric(UUID paymentId, UUID orderId, UUID customerId,
                                   BigDecimal amount, String status, Double fraudScore) {
        PaymentMetricEvent event = PaymentMetricEvent.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(status)
            .fraudScore(fraudScore)
            .timestamp(Instant.now())
            .build();

        sendEvent(PAYMENT_METRICS_TOPIC, paymentId.toString(), event);
    }

    public void recordPaymentMetric(UUID paymentId, UUID orderId, UUID customerId,
                                   BigDecimal amount, String status, String paymentMethod,
                                   Double fraudScore) {
        PaymentMetricEvent event = PaymentMetricEvent.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(status)
            .paymentMethod(paymentMethod)
            .fraudScore(fraudScore)
            .timestamp(Instant.now())
            .build();

        sendEvent(PAYMENT_METRICS_TOPIC, paymentId.toString(), event);
    }

    public void recordRevenueMetric(BigDecimal revenue, BigDecimal costs, Integer ordersCount) {
        RevenueMetricEvent event = RevenueMetricEvent.builder()
            .revenue(revenue)
            .costs(costs)
            .ordersCount(ordersCount)
            .timestamp(Instant.now())
            .build();

        sendEvent(REVENUE_METRICS_TOPIC, "revenue", event);
    }

    public void recordRevenueMetric(BigDecimal revenue, BigDecimal costs, Integer ordersCount,
                                   String region, String productCategory) {
        RevenueMetricEvent event = RevenueMetricEvent.builder()
            .revenue(revenue)
            .costs(costs)
            .ordersCount(ordersCount)
            .region(region)
            .productCategory(productCategory)
            .timestamp(Instant.now())
            .build();

        sendEvent(REVENUE_METRICS_TOPIC, "revenue", event);
    }

    public void recordSystemMetric(String serviceName, Double cpuUsage, Double memoryUsage,
                                  Double requestRate, Double errorRate, Double latencyP99) {
        SystemMetricEvent event = SystemMetricEvent.builder()
            .serviceName(serviceName)
            .cpuUsage(cpuUsage)
            .memoryUsage(memoryUsage)
            .requestRate(requestRate)
            .errorRate(errorRate)
            .latencyP99(latencyP99)
            .timestamp(Instant.now())
            .build();

        sendEvent(SYSTEM_METRICS_TOPIC, serviceName, event);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            Message<Object> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, key)
                .setHeader("ce_id", UUID.randomUUID().toString())
                .setHeader("ce_type", event.getClass().getSimpleName())
                .setHeader("ce_source", "bss-analytics-service")
                .setHeader("ce_specversion", "1.0")
                .setHeader("ce_time", Instant.now().toString())
                .build();

            kafkaTemplate.send(message);
            log.debug("Sent metric event to topic: {}", topic);
        } catch (Exception e) {
            log.error("Failed to send metric event to topic: {}", topic, e);
        }
    }

    // Event DTOs
    public static class CustomerMetricEvent {
        private UUID customerId;
        private String metricName;
        private Double metricValue;
        private Map<String, Object> labels;
        private Instant timestamp;

        // Builder
        public static class Builder {
            private CustomerMetricEvent event = new CustomerMetricEvent();

            public Builder customerId(UUID customerId) {
                event.customerId = customerId;
                return this;
            }

            public Builder metricName(String metricName) {
                event.metricName = metricName;
                return this;
            }

            public Builder metricValue(Double metricValue) {
                event.metricValue = metricValue;
                return this;
            }

            public Builder labels(Map<String, Object> labels) {
                event.labels = labels;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public CustomerMetricEvent build() {
                return event;
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getCustomerId() { return customerId; }
        public String getMetricName() { return metricName; }
        public Double getMetricValue() { return metricValue; }
        public Map<String, Object> getLabels() { return labels; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class OrderMetricEvent {
        private UUID orderId;
        private UUID customerId;
        private String status;
        private BigDecimal totalAmount;
        private Integer itemsCount;
        private String region;
        private Instant timestamp;

        // Builder
        public static class Builder {
            private OrderMetricEvent event = new OrderMetricEvent();

            public Builder orderId(UUID orderId) {
                event.orderId = orderId;
                return this;
            }

            public Builder customerId(UUID customerId) {
                event.customerId = customerId;
                return this;
            }

            public Builder status(String status) {
                event.status = status;
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

            public Builder region(String region) {
                event.region = region;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public OrderMetricEvent build() {
                return event;
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getOrderId() { return orderId; }
        public UUID getCustomerId() { return customerId; }
        public String getStatus() { return status; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public Integer getItemsCount() { return itemsCount; }
        public String getRegion() { return region; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class PaymentMetricEvent {
        private UUID paymentId;
        private UUID orderId;
        private UUID customerId;
        private BigDecimal amount;
        private String status;
        private String paymentMethod;
        private Double fraudScore;
        private Instant timestamp;

        // Builder
        public static class Builder {
            private PaymentMetricEvent event = new PaymentMetricEvent();

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

            public Builder fraudScore(Double fraudScore) {
                event.fraudScore = fraudScore;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public PaymentMetricEvent build() {
                return event;
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public UUID getPaymentId() { return paymentId; }
        public UUID getOrderId() { return orderId; }
        public UUID getCustomerId() { return customerId; }
        public BigDecimal getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getPaymentMethod() { return paymentMethod; }
        public Double getFraudScore() { return fraudScore; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class RevenueMetricEvent {
        private BigDecimal revenue;
        private BigDecimal costs;
        private Integer ordersCount;
        private String region;
        private String productCategory;
        private Instant timestamp;

        // Builder
        public static class Builder {
            private RevenueMetricEvent event = new RevenueMetricEvent();

            public Builder revenue(BigDecimal revenue) {
                event.revenue = revenue;
                return this;
            }

            public Builder costs(BigDecimal costs) {
                event.costs = costs;
                return this;
            }

            public Builder ordersCount(Integer ordersCount) {
                event.ordersCount = ordersCount;
                return this;
            }

            public Builder region(String region) {
                event.region = region;
                return this;
            }

            public Builder productCategory(String productCategory) {
                event.productCategory = productCategory;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public RevenueMetricEvent build() {
                return event;
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getCosts() { return costs; }
        public Integer getOrdersCount() { return ordersCount; }
        public String getRegion() { return region; }
        public String getProductCategory() { return productCategory; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class SystemMetricEvent {
        private String serviceName;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double requestRate;
        private Double errorRate;
        private Double latencyP99;
        private Instant timestamp;

        // Builder
        public static class Builder {
            private SystemMetricEvent event = new SystemMetricEvent();

            public Builder serviceName(String serviceName) {
                event.serviceName = serviceName;
                return this;
            }

            public Builder cpuUsage(Double cpuUsage) {
                event.cpuUsage = cpuUsage;
                return this;
            }

            public Builder memoryUsage(Double memoryUsage) {
                event.memoryUsage = memoryUsage;
                return this;
            }

            public Builder requestRate(Double requestRate) {
                event.requestRate = requestRate;
                return this;
            }

            public Builder errorRate(Double errorRate) {
                event.errorRate = errorRate;
                return this;
            }

            public Builder latencyP99(Double latencyP99) {
                event.latencyP99 = latencyP99;
                return this;
            }

            public Builder timestamp(Instant timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public SystemMetricEvent build() {
                return event;
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getServiceName() { return serviceName; }
        public Double getCpuUsage() { return cpuUsage; }
        public Double getMemoryUsage() { return memoryUsage; }
        public Double getRequestRate() { return requestRate; }
        public Double getErrorRate() { return errorRate; }
        public Double getLatencyP99() { return latencyP99; }
        public Instant getTimestamp() { return timestamp; }
    }
}
