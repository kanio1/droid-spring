package com.droid.bss.infrastructure.messaging.timeseries;

import com.droid.bss.application.service.CustomerMetricsService;
import com.droid.bss.application.service.RevenueAnalyticsService;
import com.droid.bss.application.service.FraudDetectionService;
import com.droid.bss.application.service.SystemMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricsConsumer.class);

    private final CustomerMetricsService customerMetricsService;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final FraudDetectionService fraudDetectionService;
    private final SystemMetricsService systemMetricsService;

    public MetricsConsumer(CustomerMetricsService customerMetricsService,
                          RevenueAnalyticsService revenueAnalyticsService,
                          FraudDetectionService fraudDetectionService,
                          SystemMetricsService systemMetricsService) {
        this.customerMetricsService = customerMetricsService;
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.fraudDetectionService = fraudDetectionService;
        this.systemMetricsService = systemMetricsService;
    }

    @KafkaListener(topics = "bss.customer.metrics", groupId = "timescale-ingest")
    public void handleCustomerMetric(
            @Payload MetricsProducer.CustomerMetricEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        try {
            log.debug("Received customer metric event: {}", event.getMetricName());
            customerMetricsService.recordCustomerMetric(
                event.getCustomerId(),
                event.getMetricName(),
                event.getMetricValue(),
                event.getLabels()
            );
        } catch (Exception e) {
            log.error("Failed to process customer metric event for customer: {}",
                event.getCustomerId(), e);
        }
    }

    @KafkaListener(topics = "bss.order.metrics", groupId = "timescale-ingest")
    public void handleOrderMetric(
            @Payload MetricsProducer.OrderMetricEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        try {
            log.debug("Received order metric event: {} for order: {}", event.getStatus(), event.getOrderId());
            // Would need OrderMetricsService to implement this
            // For now, just log
        } catch (Exception e) {
            log.error("Failed to process order metric event for order: {}",
                event.getOrderId(), e);
        }
    }

    @KafkaListener(topics = "bss.payment.metrics", groupId = "timescale-ingest")
    public void handlePaymentMetric(
            @Payload MetricsProducer.PaymentMetricEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        try {
            log.debug("Received payment metric event: {} for payment: {}",
                event.getStatus(), event.getPaymentId());
            fraudDetectionService.recordPaymentMetric(
                event.getPaymentId(),
                event.getOrderId(),
                event.getCustomerId(),
                event.getAmount(),
                event.getStatus(),
                event.getPaymentMethod(),
                event.getFraudScore()
            );
        } catch (Exception e) {
            log.error("Failed to process payment metric event for payment: {}",
                event.getPaymentId(), e);
        }
    }

    @KafkaListener(topics = "bss.revenue.metrics", groupId = "timescale-ingest")
    public void handleRevenueMetric(
            @Payload MetricsProducer.RevenueMetricEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        try {
            log.debug("Received revenue metric event: revenue={}, costs={}",
                event.getRevenue(), event.getCosts());
            revenueAnalyticsService.recordRevenueMetric(
                event.getRevenue(),
                event.getCosts(),
                event.getOrdersCount(),
                event.getRegion(),
                event.getProductCategory()
            );
        } catch (Exception e) {
            log.error("Failed to process revenue metric event", e);
        }
    }

    @KafkaListener(topics = "bss.system.metrics", groupId = "timescale-ingest")
    public void handleSystemMetric(
            @Payload MetricsProducer.SystemMetricEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) {
        try {
            log.debug("Received system metric event for service: {}",
                event.getServiceName());
            systemMetricsService.recordSystemMetric(
                event.getServiceName(),
                event.getCpuUsage(),
                event.getMemoryUsage(),
                event.getRequestRate(),
                event.getErrorRate(),
                event.getLatencyP99()
            );
        } catch (Exception e) {
            log.error("Failed to process system metric event for service: {}",
                event.getServiceName(), e);
        }
    }
}
