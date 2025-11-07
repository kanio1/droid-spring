package com.droid.bss.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BSS Custom Business Metrics
 * Provides observability for 400k events/minute monitoring
 */
@Component
public class BssMetrics {

    private final MeterRegistry meterRegistry;

    // Event counters
    private final Counter totalEvents;
    private final Counter eventsByType;
    private final Counter eventsByComponent;
    private final Counter eventsByTenant;

    // Business metrics
    private final Counter ordersTotal;
    private final Counter paymentsTotal;
    private final Counter invoicesTotal;
    private final Counter customersTotal;
    private final Counter subscriptionsTotal;

    // Error metrics
    private final Counter errorsTotal;
    private final Counter errorsByType;
    private final Counter kafkaErrors;
    private final Counter redisErrors;
    private final Counter postgresqlErrors;

    // Latency metrics
    private final Timer eventProcessingTimer;
    private final Timer kafkaProcessingTimer;
    private final Timer redisProcessingTimer;
    private final Timer postgresProcessingTimer;
    private final Timer orderProcessingTimer;
    private final Timer paymentProcessingTimer;

    // Throughput gauges
    private final AtomicLong currentEventRate;
    private final Gauge eventRateGauge;
    private final AtomicLong eventsProcessed;
    private final Gauge eventsProcessedGauge;

    // Distribution summaries
    private final DistributionSummary eventSizeSummary;
    private final DistributionSummary messageSizeSummary;

    public BssMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Event counters
        this.totalEvents = Counter.builder("bss_events_total")
                .description("Total number of BSS events processed")
                .register(meterRegistry);

        this.eventsByType = Counter.builder("bss_events_by_type_total")
                .description("Events grouped by type")
                .register(meterRegistry);

        this.eventsByComponent = Counter.builder("bss_events_by_component_total")
                .description("Events grouped by component (kafka, redis, postgres)")
                .register(meterRegistry);

        this.eventsByTenant = Counter.builder("bss_events_by_tenant_total")
                .description("Events grouped by tenant")
                .register(meterRegistry);

        // Business metrics
        this.ordersTotal = Counter.builder("bss_orders_total")
                .description("Total number of orders created")
                .register(meterRegistry);

        this.paymentsTotal = Counter.builder("bss_payments_total")
                .description("Total number of payments processed")
                .register(meterRegistry);

        this.invoicesTotal = Counter.builder("bss_invoices_total")
                .description("Total number of invoices generated")
                .register(meterRegistry);

        this.customersTotal = Counter.builder("bss_customers_total")
                .description("Total number of customers")
                .register(meterRegistry);

        this.subscriptionsTotal = Counter.builder("bss_subscriptions_total")
                .description("Total number of subscriptions")
                .register(meterRegistry);

        // Error metrics
        this.errorsTotal = Counter.builder("bss_errors_total")
                .description("Total number of errors")
                .register(meterRegistry);

        this.errorsByType = Counter.builder("bss_errors_by_type_total")
                .description("Errors grouped by type")
                .register(meterRegistry);

        this.kafkaErrors = Counter.builder("bss_kafka_errors_total")
                .description("Kafka-related errors")
                .register(meterRegistry);

        this.redisErrors = Counter.builder("bss_redis_errors_total")
                .description("Redis-related errors")
                .register(meterRegistry);

        this.postgresqlErrors = Counter.builder("bss_postgresql_errors_total")
                .description("PostgreSQL-related errors")
                .register(meterRegistry);

        // Timers for latency
        this.eventProcessingTimer = Timer.builder("bss_event_processing_duration_seconds")
                .description("Event processing latency")
                .register(meterRegistry);

        this.kafkaProcessingTimer = Timer.builder("bss_kafka_processing_duration_seconds")
                .description("Kafka processing latency")
                .register(meterRegistry);

        this.redisProcessingTimer = Timer.builder("bss_redis_processing_duration_seconds")
                .description("Redis processing latency")
                .register(meterRegistry);

        this.postgresProcessingTimer = Timer.builder("bss_postgres_processing_duration_seconds")
                .description("PostgreSQL processing latency")
                .register(meterRegistry);

        this.orderProcessingTimer = Timer.builder("bss_order_processing_duration_seconds")
                .description("Order processing latency")
                .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("bss_payment_processing_duration_seconds")
                .description("Payment processing latency")
                .register(meterRegistry);

        // Throughput gauges
        this.currentEventRate = new AtomicLong(0);
        this.eventRateGauge = Gauge.builder("bss_current_event_rate", currentEventRate, AtomicLong::doubleValue)
                .description("Current event processing rate (events/sec)")
                .register(meterRegistry);

        this.eventsProcessed = new AtomicLong(0);
        this.eventsProcessedGauge = Gauge.builder("bss_events_processed_total", eventsProcessed, AtomicLong::doubleValue)
                .description("Total events processed")
                .register(meterRegistry);

        // Distribution summaries
        this.eventSizeSummary = DistributionSummary.builder("bss_event_size_bytes")
                .description("Event size in bytes")
                .register(meterRegistry);

        this.messageSizeSummary = DistributionSummary.builder("bss_message_size_bytes")
                .description("Message size in bytes")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        // Initialize any starting values
    }

    // Event metrics
    public void recordEvent() {
        totalEvents.increment();
        eventsProcessed.incrementAndGet();
    }

    public void recordEvent(String eventType, String component, String tenantId) {
        totalEvents.increment();
        eventsProcessed.incrementAndGet();

        // Note: Counter.increment() doesn't support tag parameters in this version
        // Tags are configured at Counter creation time
        eventsByType.increment();
        eventsByComponent.increment();
        eventsByTenant.increment();
    }

    public void recordEventSize(long size) {
        eventSizeSummary.record(size);
    }

    public void recordMessageSize(long size) {
        messageSizeSummary.record(size);
    }

    // Business metrics
    public void recordOrder() {
        ordersTotal.increment();
    }

    public void recordPayment() {
        paymentsTotal.increment();
    }

    public void recordInvoice() {
        invoicesTotal.increment();
    }

    public void recordCustomer() {
        customersTotal.increment();
    }

    public void recordSubscription() {
        subscriptionsTotal.increment();
    }

    // Error metrics
    public void recordError() {
        errorsTotal.increment();
    }

    public void recordError(String errorType) {
        errorsTotal.increment();
        errorsByType.increment();
    }

    public void recordKafkaError() {
        kafkaErrors.increment();
    }

    public void recordRedisError() {
        redisErrors.increment();
    }

    public void recordPostgreSQLError() {
        postgresqlErrors.increment();
    }

    // Timer methods
    public void recordEventProcessingTime(long durationNanos) {
        eventProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordKafkaProcessingTime(long durationNanos) {
        kafkaProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordRedisProcessingTime(long durationNanos) {
        redisProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordPostgresProcessingTime(long durationNanos) {
        postgresProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordOrderProcessingTime(long durationNanos) {
        orderProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    public void recordPaymentProcessingTime(long durationNanos) {
        paymentProcessingTimer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    // Update current rate
    public void updateEventRate(long rate) {
        currentEventRate.set(rate);
    }
}
