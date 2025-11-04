package com.droid.bss.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Business metrics for monitoring key business operations
 */
@Component
public class BusinessMetrics {

    // Counters for tracking business events
    private final Counter customerCreatedCounter;
    private final Counter customerUpdatedCounter;
    private final Counter customerStatusChangedCounter;
    private final Counter invoiceCreatedCounter;
    private final Counter invoicePaidCounter;
    private final Counter orderCreatedCounter;
    private final Counter paymentCreatedCounter;
    private final Counter paymentCompletedCounter;
    private final Counter subscriptionCreatedCounter;
    private final Counter subscriptionRenewedCounter;

    // Timers for tracking operation latency
    private final Timer invoiceProcessingTimer;
    private final Timer paymentProcessingTimer;
    private final Timer customerQueryTimer;
    private final Timer serviceActivationTimer;
    private final Timer serviceDeactivationTimer;

    // Gauges for tracking current state
    private final AtomicLong activeSubscriptionsGauge;
    private final AtomicLong pendingInvoicesGauge;
    private final AtomicLong totalCustomersGauge;
    private final AtomicLong activeServicesGauge;
    private final AtomicLong pendingActivationsGauge;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        // Customer metrics
        this.customerCreatedCounter = Counter.builder("bss.customers.created.total")
                .description("Total number of customers created")
                .register(meterRegistry);
        this.customerUpdatedCounter = Counter.builder("bss.customers.updated.total")
                .description("Total number of customers updated")
                .register(meterRegistry);
        this.customerStatusChangedCounter = Counter.builder("bss.customers.status_changed.total")
                .description("Total number of customer status changes")
                .register(meterRegistry);

        // Invoice metrics
        this.invoiceCreatedCounter = Counter.builder("bss.invoices.created.total")
                .description("Total number of invoices created")
                .register(meterRegistry);
        this.invoicePaidCounter = Counter.builder("bss.invoices.paid.total")
                .description("Total number of invoices paid")
                .register(meterRegistry);

        // Order metrics
        this.orderCreatedCounter = Counter.builder("bss.orders.created.total")
                .description("Total number of orders created")
                .register(meterRegistry);

        // Payment metrics
        this.paymentCreatedCounter = Counter.builder("bss.payments.created.total")
                .description("Total number of payments created")
                .register(meterRegistry);
        this.paymentCompletedCounter = Counter.builder("bss.payments.completed.total")
                .description("Total number of payments completed")
                .register(meterRegistry);

        // Subscription metrics
        this.subscriptionCreatedCounter = Counter.builder("bss.subscriptions.created.total")
                .description("Total number of subscriptions created")
                .register(meterRegistry);
        this.subscriptionRenewedCounter = Counter.builder("bss.subscriptions.renewed.total")
                .description("Total number of subscriptions renewed")
                .register(meterRegistry);

        // Timers
        this.invoiceProcessingTimer = Timer.builder("bss.invoices.processing.duration")
                .description("Time taken to process invoices")
                .register(meterRegistry);
        this.paymentProcessingTimer = Timer.builder("bss.payments.processing.duration")
                .description("Time taken to process payments")
                .register(meterRegistry);
        this.customerQueryTimer = Timer.builder("bss.customers.query.duration")
                .description("Time taken to query customers")
                .register(meterRegistry);
        this.serviceActivationTimer = Timer.builder("bss.services.activation.duration")
                .description("Time taken to activate services")
                .register(meterRegistry);
        this.serviceDeactivationTimer = Timer.builder("bss.services.deactivation.duration")
                .description("Time taken to deactivate services")
                .register(meterRegistry);

        // Gauges
        this.activeSubscriptionsGauge = meterRegistry.gauge("bss.subscriptions.active", new AtomicLong(0));
        this.pendingInvoicesGauge = meterRegistry.gauge("bss.invoices.pending", new AtomicLong(0));
        this.totalCustomersGauge = meterRegistry.gauge("bss.customers.total", new AtomicLong(0));
        this.activeServicesGauge = meterRegistry.gauge("bss.services.active", new AtomicLong(0));
        this.pendingActivationsGauge = meterRegistry.gauge("bss.services.pending_activations", new AtomicLong(0));
    }

    // Customer metrics methods
    public void incrementCustomerCreated() {
        customerCreatedCounter.increment();
        totalCustomersGauge.incrementAndGet();
    }

    public void incrementCustomerUpdated() {
        customerUpdatedCounter.increment();
    }

    public void incrementCustomerStatusChanged() {
        customerStatusChangedCounter.increment();
    }

    // Invoice metrics methods
    public void incrementInvoiceCreated() {
        invoiceCreatedCounter.increment();
        pendingInvoicesGauge.incrementAndGet();
    }

    public void incrementInvoicePaid() {
        invoicePaidCounter.increment();
        pendingInvoicesGauge.decrementAndGet();
    }

    public Timer.Sample startInvoiceProcessing() {
        return Timer.start();
    }

    public void recordInvoiceProcessing(Timer.Sample sample) {
        sample.stop(invoiceProcessingTimer);
    }

    // Order metrics methods
    public void incrementOrderCreated() {
        orderCreatedCounter.increment();
    }

    // Payment metrics methods
    public void incrementPaymentCreated() {
        paymentCreatedCounter.increment();
    }

    public void incrementPaymentCompleted() {
        paymentCompletedCounter.increment();
    }

    public Timer.Sample startPaymentProcessing() {
        return Timer.start();
    }

    public void recordPaymentProcessing(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }

    // Subscription metrics methods
    public void incrementSubscriptionCreated() {
        subscriptionCreatedCounter.increment();
        activeSubscriptionsGauge.incrementAndGet();
    }

    public void incrementSubscriptionRenewed() {
        subscriptionRenewedCounter.increment();
    }

    // Query metrics
    public Timer.Sample startCustomerQuery() {
        return Timer.start();
    }

    public void recordCustomerQuery(Timer.Sample sample) {
        sample.stop(customerQueryTimer);
    }

    // Gauge update methods
    public void setActiveSubscriptions(long count) {
        activeSubscriptionsGauge.set(count);
    }

    public void setPendingInvoices(long count) {
        pendingInvoicesGauge.set(count);
    }

    public void setTotalCustomers(long count) {
        totalCustomersGauge.set(count);
    }

    // Service metrics methods
    public Timer.Sample startServiceActivation() {
        return Timer.start();
    }

    public void recordServiceActivation(Timer.Sample sample) {
        sample.stop(serviceActivationTimer);
    }

    public Timer.Sample startServiceDeactivation() {
        return Timer.start();
    }

    public void recordServiceDeactivation(Timer.Sample sample) {
        sample.stop(serviceDeactivationTimer);
    }

    // Gauge update methods
    public void setActiveServices(long count) {
        activeServicesGauge.set(count);
    }

    public void setPendingActivations(long count) {
        pendingActivationsGauge.set(count);
    }
}
