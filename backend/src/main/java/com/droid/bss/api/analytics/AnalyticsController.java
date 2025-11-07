package com.droid.bss.api.analytics;

import com.droid.bss.application.service.RevenueAnalyticsService;
import com.droid.bss.application.service.CustomerMetricsService;
import com.droid.bss.application.service.SystemMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Time-series analytics endpoints")
public class AnalyticsController {

    private final RevenueAnalyticsService revenueAnalyticsService;
    private final CustomerMetricsService customerMetricsService;
    private final SystemMetricsService systemMetricsService;

    public AnalyticsController(RevenueAnalyticsService revenueAnalyticsService,
                              CustomerMetricsService customerMetricsService,
                              SystemMetricsService systemMetricsService) {
        this.revenueAnalyticsService = revenueAnalyticsService;
        this.customerMetricsService = customerMetricsService;
        this.systemMetricsService = systemMetricsService;
    }

    @GetMapping("/revenue/summary")
    @Operation(summary = "Get revenue summary", description = "Get daily revenue summary for the last N days")
    public ResponseEntity<RevenueAnalyticsService.RevenueSummary> getRevenueSummary(
            @Parameter(description = "Number of days to include", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        RevenueAnalyticsService.RevenueSummary summary = revenueAnalyticsService.getRevenueSummary(days);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/revenue/region")
    @Operation(summary = "Get revenue by region", description = "Get revenue breakdown by region")
    public ResponseEntity<List<RevenueAnalyticsService.RevenueByRegion>> getRevenueByRegion(
            @Parameter(description = "Number of days to include", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        List<RevenueAnalyticsService.RevenueByRegion> revenue = revenueAnalyticsService.getRevenueByRegion(days);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/revenue/category")
    @Operation(summary = "Get revenue by product category", description = "Get revenue breakdown by product category")
    public ResponseEntity<List<RevenueAnalyticsService.RevenueByCategory>> getRevenueByCategory(
            @Parameter(description = "Number of days to include", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        List<RevenueAnalyticsService.RevenueByCategory> revenue = revenueAnalyticsService.getRevenueByCategory(days);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/revenue/forecast")
    @Operation(summary = "Get revenue forecast", description = "Forecast revenue for the next N days")
    public ResponseEntity<RevenueAnalyticsService.RevenueForecast> forecastRevenue(
            @Parameter(description = "Number of days to forecast", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        RevenueAnalyticsService.RevenueForecast forecast = revenueAnalyticsService.forecastRevenue(days);
        return ResponseEntity.ok(forecast);
    }

    @GetMapping("/revenue/total")
    @Operation(summary = "Get total revenue", description = "Get total revenue for the last N days")
    public ResponseEntity<java.math.BigDecimal> getTotalRevenue(
            @Parameter(description = "Number of days", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        java.math.BigDecimal total = revenueAnalyticsService.getTotalRevenue(days);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/revenue/profit")
    @Operation(summary = "Get total profit", description = "Get total profit for the last N days")
    public ResponseEntity<java.math.BigDecimal> getTotalProfit(
            @Parameter(description = "Number of days", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        java.math.BigDecimal total = revenueAnalyticsService.getTotalProfit(days);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/revenue/aov")
    @Operation(summary = "Get average order value", description = "Get average order value for the last N days")
    public ResponseEntity<java.math.BigDecimal> getAverageOrderValue(
            @Parameter(description = "Number of days", example = "30")
            @RequestParam(defaultValue = "30") int days) {

        java.math.BigDecimal aov = revenueAnalyticsService.getAverageOrderValue(days);
        return ResponseEntity.ok(aov);
    }

    @GetMapping("/customer/metrics/{customerId}")
    @Operation(summary = "Get customer metrics", description = "Get time-series metrics for a specific customer")
    public ResponseEntity<List<CustomerMetricsService.CustomerMetric>> getCustomerMetrics(
            @Parameter(description = "Customer ID") @PathVariable UUID customerId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {

        List<CustomerMetricsService.CustomerMetric> metrics =
            customerMetricsService.getCustomerMetrics(customerId, startTime, endTime);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/customer/top")
    @Operation(summary = "Get top customers by activity", description = "Get top N customers by activity count")
    public ResponseEntity<List<CustomerMetricsService.CustomerActivity>> getTopCustomers(
            @Parameter(description = "Number of days", example = "7")
            @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "Number of customers to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        List<CustomerMetricsService.CustomerActivity> customers =
            customerMetricsService.getTopCustomers(days, limit);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/customer/metrics/{customerId}/aggregates")
    @Operation(summary = "Get customer metric aggregates", description = "Get aggregated metrics for a customer")
    public ResponseEntity<List<CustomerMetricsService.MetricAggregate>> getCustomerMetricAggregates(
            @Parameter(description = "Customer ID") @PathVariable UUID customerId,
            @Parameter(description = "Metric name", example = "login_count") @RequestParam String metricName,
            @Parameter(description = "Time period (minute, hour, day, week, month)", example = "day")
            @RequestParam String period,
            @Parameter(description = "Number of periods", example = "7") @RequestParam(defaultValue = "7") int periods) {

        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(getPeriodSeconds(period) * periods);

        List<CustomerMetricsService.MetricAggregate> aggregates =
            customerMetricsService.getMetricAggregates(metricName, period, startTime, endTime);
        return ResponseEntity.ok(aggregates);
    }

    @GetMapping("/system/health")
    @Operation(summary = "Get system health status", description = "Get overall system health status")
    public ResponseEntity<SystemMetricsService.SystemHealthStatus> getSystemHealth(
            @Parameter(description = "Number of hours to analyze", example = "1")
            @RequestParam(defaultValue = "1") int hours) {

        SystemMetricsService.SystemHealthStatus status = systemMetricsService.getSystemHealthStatus(hours);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/system/performance/{serviceName}")
    @Operation(summary = "Get performance trends for a service", description = "Get performance trends for a specific service")
    public ResponseEntity<List<SystemMetricsService.PerformanceTrend>> getPerformanceTrends(
            @Parameter(description = "Service name", example = "backend-api") @PathVariable String serviceName,
            @Parameter(description = "Number of hours", example = "24")
            @RequestParam(defaultValue = "24") int hours) {

        List<SystemMetricsService.PerformanceTrend> trends =
            systemMetricsService.getPerformanceTrends(serviceName, hours);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/system/comparison")
    @Operation(summary = "Get service comparison", description = "Compare performance across all services")
    public ResponseEntity<List<SystemMetricsService.ServiceComparison>> getServiceComparison(
            @Parameter(description = "Number of hours", example = "24")
            @RequestParam(defaultValue = "24") int hours) {

        List<SystemMetricsService.ServiceComparison> comparison =
            systemMetricsService.getServiceComparison(hours);
        return ResponseEntity.ok(comparison);
    }

    private long getPeriodSeconds(String period) {
        return switch (period.toLowerCase()) {
            case "minute" -> 60L;
            case "hour" -> 60L * 60;
            case "day" -> 24L * 60 * 60;
            case "week" -> 7L * 24 * 60 * 60;
            case "month" -> 30L * 24 * 60 * 60;
            default -> 24L * 60 * 60;
        };
    }
}
