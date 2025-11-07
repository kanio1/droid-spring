package com.droid.bss.api.analytics;

import com.droid.bss.application.service.CustomerMetricsService;
import com.droid.bss.application.service.RevenueAnalyticsService;
import com.droid.bss.application.service.FraudDetectionService;
import com.droid.bss.application.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Time-series metrics recording endpoints")
public class MetricsController {

    private final CustomerMetricsService customerMetricsService;
    private final RevenueAnalyticsService revenueAnalyticsService;
    private final FraudDetectionService fraudDetectionService;
    private final SystemMetricsService systemMetricsService;

    public MetricsController(CustomerMetricsService customerMetricsService,
                            RevenueAnalyticsService revenueAnalyticsService,
                            FraudDetectionService fraudDetectionService,
                            SystemMetricsService systemMetricsService) {
        this.customerMetricsService = customerMetricsService;
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.fraudDetectionService = fraudDetectionService;
        this.systemMetricsService = systemMetricsService;
    }

    @PostMapping("/customer")
    @Operation(summary = "Record customer metric", description = "Record a customer activity metric")
    public ResponseEntity<Void> recordCustomerMetric(
            @RequestParam UUID customerId,
            @RequestParam String metricName,
            @RequestParam Double metricValue,
            @RequestBody(required = false) Map<String, Object> labels) {

        customerMetricsService.recordCustomerMetric(customerId, metricName, metricValue, labels);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order")
    @Operation(summary = "Record order metric", description = "Record an order metric")
    public ResponseEntity<Void> recordOrderMetric(
            @RequestParam UUID orderId,
            @RequestParam UUID customerId,
            @RequestParam String status,
            @RequestParam BigDecimal totalAmount,
            @RequestParam Integer itemsCount,
            @RequestParam(required = false) String region) {

        // Record in order metrics table
        // This would need OrderMetricsRepository and Service
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payment")
    @Operation(summary = "Record payment metric", description = "Record a payment metric for fraud detection")
    public ResponseEntity<Void> recordPaymentMetric(
            @RequestParam UUID paymentId,
            @RequestParam UUID orderId,
            @RequestParam UUID customerId,
            @RequestParam BigDecimal amount,
            @RequestParam String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Double fraudScore) {

        fraudDetectionService.recordPaymentMetric(
            paymentId, orderId, customerId, amount, status, paymentMethod, fraudScore);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/revenue")
    @Operation(summary = "Record revenue metric", description = "Record a revenue metric")
    public ResponseEntity<Void> recordRevenueMetric(
            @RequestParam BigDecimal revenue,
            @RequestParam BigDecimal costs,
            @RequestParam Integer ordersCount,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String productCategory) {

        revenueAnalyticsService.recordRevenueMetric(revenue, costs, ordersCount, region, productCategory);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/system")
    @Operation(summary = "Record system metric", description = "Record a system performance metric")
    public ResponseEntity<Void> recordSystemMetric(
            @RequestParam String serviceName,
            @RequestParam Double cpuUsage,
            @RequestParam Double memoryUsage,
            @RequestParam Double requestRate,
            @RequestParam Double errorRate,
            @RequestParam Double latencyP99) {

        systemMetricsService.recordSystemMetric(
            serviceName, cpuUsage, memoryUsage, requestRate, errorRate, latencyP99);
        return ResponseEntity.ok().build();
    }
}
