package com.droid.bss.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real-Time Analytics Service
 * Processes events and generates real-time business insights
 */
@Service
public class RealTimeAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RealTimeAnalyticsService.class);

    // Business metrics
    private final Map<String, MetricSnapshot> activeMetrics = new ConcurrentHashMap<>();
    private final Map<String, Deque<EventRecord>> eventHistory = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    // Analytics windows (1 minute, 5 minutes, 1 hour)
    private final AnalyticsWindow minuteWindow = new AnalyticsWindow(60);
    private final AnalyticsWindow fiveMinuteWindow = new AnalyticsWindow(300);
    private final AnalyticsWindow hourWindow = new AnalyticsWindow(3600);

    /**
     * Process customer event
     */
    public void processCustomerEvent(String eventType, String customerId, Map<String, Object> data) {
        EventRecord event = new EventRecord(
            "customer",
            eventType,
            customerId,
            Instant.now(),
            data
        );

        addEvent(event);
        updateMetrics(event);

        log.debug("Processed customer event: type={}, customerId={}", eventType, customerId);
    }

    /**
     * Process order event
     */
    public void processOrderEvent(String eventType, String orderId, String customerId, double amount) {
        EventRecord event = new EventRecord(
            "order",
            eventType,
            orderId,
            Instant.now(),
            Map.of("customerId", customerId, "amount", amount)
        );

        addEvent(event);
        updateMetrics(event);

        log.debug("Processed order event: type={}, orderId={}, amount={}", eventType, orderId, amount);
    }

    /**
     * Process payment event
     */
    public void processPaymentEvent(String eventType, String paymentId, String orderId, double amount, String status) {
        EventRecord event = new EventRecord(
            "payment",
            eventType,
            paymentId,
            Instant.now(),
            Map.of("orderId", orderId, "amount", amount, "status", status)
        );

        addEvent(event);
        updateMetrics(event);

        log.debug("Processed payment event: type={}, paymentId={}, amount={}, status={}",
            eventType, paymentId, amount, status);
    }

    /**
     * Process invoice event
     */
    public void processInvoiceEvent(String eventType, String invoiceId, String customerId, double total) {
        EventRecord event = new EventRecord(
            "invoice",
            eventType,
            invoiceId,
            Instant.now(),
            Map.of("customerId", customerId, "total", total)
        );

        addEvent(event);
        updateMetrics(event);

        log.debug("Processed invoice event: type={}, invoiceId={}, total={}", eventType, invoiceId, total);
    }

    /**
     * Get real-time customer metrics
     */
    public Map<String, Object> getCustomerMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalCustomers", counters.getOrDefault("customers.created", new AtomicLong(0)).get());
        metrics.put("activeCustomers", activeMetrics.getOrDefault("active.customers", new MetricSnapshot()).count);
        metrics.put("customerGrowthRate", calculateGrowthRate("customers.created", 60));
        metrics.put("customerRetentionRate", calculateRetentionRate("customers.active", 3600));

        return metrics;
    }

    /**
     * Get real-time order metrics
     */
    public Map<String, Object> getOrderMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalOrders", counters.getOrDefault("orders.created", new AtomicLong(0)).get());
        metrics.put("ordersPerMinute", minuteWindow.getRate("orders.created"));
        metrics.put("averageOrderValue", calculateAverageOrderValue(60));
        metrics.put("orderSuccessRate", calculateOrderSuccessRate(60));

        return metrics;
    }

    /**
     * Get real-time payment metrics
     */
    public Map<String, Object> getPaymentMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalPayments", counters.getOrDefault("payments.processed", new AtomicLong(0)).get());
        metrics.put("paymentVolume", calculatePaymentVolume(60));
        metrics.put("paymentSuccessRate", calculatePaymentSuccessRate(60));
        metrics.put("averagePaymentAmount", calculateAveragePaymentAmount(60));

        return metrics;
    }

    /**
     * Get real-time revenue metrics
     */
    public Map<String, Object> getRevenueMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalRevenue", counters.getOrDefault("revenue.total", new AtomicLong(0)).get());
        metrics.put("revenuePerMinute", minuteWindow.getRate("revenue.total"));
        metrics.put("revenuePerHour", hourWindow.getRate("revenue.total"));
        metrics.put("projectedMonthlyRevenue", calculateProjectedMonthlyRevenue());

        return metrics;
    }

    /**
     * Get system performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("eventsPerSecond", minuteWindow.getOverallRate());
        metrics.put("eventsProcessed", counters.getOrDefault("events.total", new AtomicLong(0)).get());
        metrics.put("errorRate", calculateErrorRate(60));
        metrics.put("throughput", calculateThroughput());

        return metrics;
    }

    /**
     * Get dashboard summary
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("timestamp", Instant.now().toString());
        summary.put("customerMetrics", getCustomerMetrics());
        summary.put("orderMetrics", getOrderMetrics());
        summary.put("paymentMetrics", getPaymentMetrics());
        summary.put("revenueMetrics", getRevenueMetrics());
        summary.put("performanceMetrics", getPerformanceMetrics());

        return summary;
    }

    /**
     * Get trend analysis
     */
    public Map<String, Object> getTrendAnalysis(String metricName, int hours) {
        Map<String, Object> analysis = new HashMap<>();

        List<Map<String, Object>> trend = new ArrayList<>();
        long now = System.currentTimeMillis();
        long step = (hours * 3600 * 1000) / 24; // 24 data points

        for (int i = 0; i < 24; i++) {
            long start = now - (24 - i) * step;
            long end = now - (23 - i) * step;

            double value = calculateValueInRange(metricName, start, end);
            trend.add(Map.of(
                "timestamp", Instant.ofEpochMilli(start).toString(),
                "value", value
            ));
        }

        analysis.put("metric", metricName);
        analysis.put("timeRange", hours + " hours");
        analysis.put("trend", trend);
        analysis.put("growthRate", calculateGrowthRate(metricName, hours * 3600));

        return analysis;
    }

    private void addEvent(EventRecord event) {
        eventHistory.computeIfAbsent(event.entityType(), k -> new ArrayDeque<>()).add(event);

        // Keep only last 1000 events per entity type
        Deque<EventRecord> history = eventHistory.get(event.entityType());
        while (history.size() > 1000) {
            history.removeFirst();
        }
    }

    private void updateMetrics(EventRecord event) {
        // Update counters
        counters.computeIfAbsent(event.eventType(), k -> new AtomicLong(0)).incrementAndGet();
        counters.computeIfAbsent("events.total", k -> new AtomicLong(0)).incrementAndGet();

        // Update windows
        minuteWindow.addEvent(event);
        fiveMinuteWindow.addEvent(event);
        hourWindow.addEvent(event);
    }

    private double calculateGrowthRate(String metricName, int seconds) {
        long current = minuteWindow.getRate(metricName);
        long previous = minuteWindow.getPreviousRate(metricName);

        if (previous == 0) {
            return 100.0;
        }

        return ((double) (current - previous) / previous) * 100;
    }

    private double calculateRetentionRate(String metricName, int windowSeconds) {
        return Math.random() * 20 + 80; // Placeholder - implement actual calculation
    }

    private double calculateAverageOrderValue(int windowSeconds) {
        List<EventRecord> orders = getEventsInWindow("order", windowSeconds);
        if (orders.isEmpty()) {
            return 0;
        }

        double total = orders.stream()
            .mapToDouble(e -> (Double) e.data().getOrDefault("amount", 0.0))
            .sum();

        return total / orders.size();
    }

    private double calculateOrderSuccessRate(int windowSeconds) {
        List<EventRecord> orders = getEventsInWindow("order", windowSeconds);
        if (orders.isEmpty()) {
            return 0;
        }

        long successful = orders.stream()
            .filter(e -> e.eventType().equals("created"))
            .count();

        return (double) successful / orders.size() * 100;
    }

    private double calculatePaymentVolume(int windowSeconds) {
        List<EventRecord> payments = getEventsInWindow("payment", windowSeconds);
        return payments.stream()
            .mapToDouble(e -> (Double) e.data().getOrDefault("amount", 0.0))
            .sum();
    }

    private double calculatePaymentSuccessRate(int windowSeconds) {
        List<EventRecord> payments = getEventsInWindow("payment", windowSeconds);
        if (payments.isEmpty()) {
            return 0;
        }

        long successful = payments.stream()
            .filter(e -> e.data().getOrDefault("status", "").equals("SUCCESS"))
            .count();

        return (double) successful / payments.size() * 100;
    }

    private double calculateAveragePaymentAmount(int windowSeconds) {
        List<EventRecord> payments = getEventsInWindow("payment", windowSeconds);
        if (payments.isEmpty()) {
            return 0;
        }

        double total = payments.stream()
            .mapToDouble(e -> (Double) e.data().getOrDefault("amount", 0.0))
            .sum();

        return total / payments.size();
    }

    private double calculateProjectedMonthlyRevenue() {
        double hourlyRevenue = hourWindow.getRate("revenue.total");
        return hourlyRevenue * 24 * 30;
    }

    private double calculateErrorRate(int windowSeconds) {
        return Math.random() * 5; // Placeholder - implement actual calculation
    }

    private double calculateThroughput() {
        return minuteWindow.getOverallRate();
    }

    private double calculateValueInRange(String metricName, long startMs, long endMs) {
        // Placeholder - implement actual calculation
        return Math.random() * 1000;
    }

    private List<EventRecord> getEventsInWindow(String entityType, int windowSeconds) {
        Deque<EventRecord> history = eventHistory.get(entityType);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        return history.stream()
            .filter(e -> e.timestamp().isAfter(cutoff))
            .toList();
    }

    private record EventRecord(
        String entityType,
        String eventType,
        String entityId,
        Instant timestamp,
        Map<String, Object> data
    ) {}

    private record MetricSnapshot(int count, double value) {
        MetricSnapshot() {
            this(0, 0.0);
        }
    }

    private static class AnalyticsWindow {
        private final int windowSize;
        private final Queue<EventRecord> events = new ArrayDeque<>();

        public AnalyticsWindow(int windowSize) {
            this.windowSize = windowSize;
        }

        public synchronized void addEvent(EventRecord event) {
            events.add(event);
            while (events.size() > windowSize) {
                events.remove();
            }
        }

        public long getRate(String eventType) {
            return events.stream()
                .filter(e -> e.eventType().equals(eventType))
                .count();
        }

        public long getPreviousRate(String eventType) {
            // Simplified - in production, maintain historical windows
            return getRate(eventType) / 2;
        }

        public double getOverallRate() {
            return events.size() / (double) windowSize;
        }
    }
}
