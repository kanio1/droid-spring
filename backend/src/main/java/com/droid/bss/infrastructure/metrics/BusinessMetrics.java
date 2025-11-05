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

    // Billing metrics
    private final Counter usageRecordIngestedCounter;
    private final Counter usageRecordRatedCounter;
    private final Counter billingCycleStartedCounter;
    private final Counter billingCycleProcessedCounter;
    private final Counter ratingRuleMatchedCounter;
    private final Timer usageRatingTimer;
    private final Timer billingCycleProcessingTimer;

    // Asset management metrics
    private final Counter assetCreatedCounter;
    private final Counter assetAssignedCounter;
    private final Counter assetReleasedCounter;
    private final Counter networkElementCreatedCounter;
    private final Counter networkElementHeartbeatCounter;
    private final Counter simCardCreatedCounter;
    private final Counter simCardAssignedCounter;
    private final Timer assetOperationTimer;

    // Gauges for tracking current state
    private final AtomicLong activeSubscriptionsGauge;
    private final AtomicLong pendingInvoicesGauge;
    private final AtomicLong totalCustomersGauge;
    private final AtomicLong activeServicesGauge;
    private final AtomicLong pendingActivationsGauge;
    private final AtomicLong unratedUsageGauge;
    private final AtomicLong pendingBillingCyclesGauge;
    private final AtomicLong totalAssetsGauge;
    private final AtomicLong availableAssetsGauge;
    private final AtomicLong assetsInUseGauge;
    private final AtomicLong totalSIMCardsGauge;
    private final AtomicLong availableSIMCardsGauge;
    private final AtomicLong networkElementsOnlineGauge;

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

        // Billing metrics
        this.usageRecordIngestedCounter = Counter.builder("bss.billing.usage_records.ingested.total")
                .description("Total number of usage records ingested")
                .register(meterRegistry);
        this.usageRecordRatedCounter = Counter.builder("bss.billing.usage_records.rated.total")
                .description("Total number of usage records rated")
                .register(meterRegistry);
        this.billingCycleStartedCounter = Counter.builder("bss.billing.cycles.started.total")
                .description("Total number of billing cycles started")
                .register(meterRegistry);
        this.billingCycleProcessedCounter = Counter.builder("bss.billing.cycles.processed.total")
                .description("Total number of billing cycles processed")
                .register(meterRegistry);
        this.ratingRuleMatchedCounter = Counter.builder("bss.billing.rating.rules.matched.total")
                .description("Total number of rating rules matched")
                .register(meterRegistry);
        this.usageRatingTimer = Timer.builder("bss.billing.usage_rating.duration")
                .description("Time taken to rate usage records")
                .register(meterRegistry);
        this.billingCycleProcessingTimer = Timer.builder("bss.billing.cycle_processing.duration")
                .description("Time taken to process billing cycles")
                .register(meterRegistry);

        // Asset management metrics
        this.assetCreatedCounter = Counter.builder("bss.assets.created.total")
                .description("Total number of assets created")
                .register(meterRegistry);
        this.assetAssignedCounter = Counter.builder("bss.assets.assigned.total")
                .description("Total number of assets assigned")
                .register(meterRegistry);
        this.assetReleasedCounter = Counter.builder("bss.assets.released.total")
                .description("Total number of assets released")
                .register(meterRegistry);
        this.networkElementCreatedCounter = Counter.builder("bss.assets.network_elements.created.total")
                .description("Total number of network elements created")
                .register(meterRegistry);
        this.networkElementHeartbeatCounter = Counter.builder("bss.assets.network_elements.heartbeat.total")
                .description("Total number of network element heartbeats")
                .register(meterRegistry);
        this.simCardCreatedCounter = Counter.builder("bss.assets.sim_cards.created.total")
                .description("Total number of SIM cards created")
                .register(meterRegistry);
        this.simCardAssignedCounter = Counter.builder("bss.assets.sim_cards.assigned.total")
                .description("Total number of SIM cards assigned")
                .register(meterRegistry);
        this.assetOperationTimer = Timer.builder("bss.assets.operation.duration")
                .description("Time taken to perform asset operations")
                .register(meterRegistry);

        // Gauges
        this.activeSubscriptionsGauge = meterRegistry.gauge("bss.subscriptions.active", new AtomicLong(0));
        this.pendingInvoicesGauge = meterRegistry.gauge("bss.invoices.pending", new AtomicLong(0));
        this.totalCustomersGauge = meterRegistry.gauge("bss.customers.total", new AtomicLong(0));
        this.activeServicesGauge = meterRegistry.gauge("bss.services.active", new AtomicLong(0));
        this.pendingActivationsGauge = meterRegistry.gauge("bss.services.pending_activations", new AtomicLong(0));
        this.unratedUsageGauge = meterRegistry.gauge("bss.billing.usage.unrated", new AtomicLong(0));
        this.pendingBillingCyclesGauge = meterRegistry.gauge("bss.billing.cycles.pending", new AtomicLong(0));
        this.totalAssetsGauge = meterRegistry.gauge("bss.assets.total", new AtomicLong(0));
        this.availableAssetsGauge = meterRegistry.gauge("bss.assets.available", new AtomicLong(0));
        this.assetsInUseGauge = meterRegistry.gauge("bss.assets.in_use", new AtomicLong(0));
        this.totalSIMCardsGauge = meterRegistry.gauge("bss.assets.sim_cards.total", new AtomicLong(0));
        this.availableSIMCardsGauge = meterRegistry.gauge("bss.assets.sim_cards.available", new AtomicLong(0));
        this.networkElementsOnlineGauge = meterRegistry.gauge("bss.assets.network_elements.online", new AtomicLong(0));
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

    // Billing metrics methods
    public void incrementUsageRecordIngested() {
        usageRecordIngestedCounter.increment();
    }

    public void incrementUsageRecordRated() {
        usageRecordRatedCounter.increment();
    }

    public void incrementBillingCycleStarted() {
        billingCycleStartedCounter.increment();
        pendingBillingCyclesGauge.incrementAndGet();
    }

    public void incrementBillingCycleProcessed() {
        billingCycleProcessedCounter.increment();
        pendingBillingCyclesGauge.decrementAndGet();
    }

    public void incrementRatingRuleMatched() {
        ratingRuleMatchedCounter.increment();
    }

    public Timer.Sample startUsageRating() {
        return Timer.start();
    }

    public void recordUsageRating(Timer.Sample sample) {
        sample.stop(usageRatingTimer);
    }

    public Timer.Sample startBillingCycleProcessing() {
        return Timer.start();
    }

    public void recordBillingCycleProcessing(Timer.Sample sample) {
        sample.stop(billingCycleProcessingTimer);
    }

    // Billing gauge update methods
    public void setUnratedUsage(long count) {
        unratedUsageGauge.set(count);
    }

    public void setPendingBillingCycles(long count) {
        pendingBillingCyclesGauge.set(count);
    }

    // Asset management metrics methods
    public void incrementAssetCreated() {
        assetCreatedCounter.increment();
        totalAssetsGauge.incrementAndGet();
        availableAssetsGauge.incrementAndGet();
    }

    public void incrementAssetAssigned() {
        assetAssignedCounter.increment();
        availableAssetsGauge.decrementAndGet();
        assetsInUseGauge.incrementAndGet();
    }

    public void incrementAssetReleased() {
        assetReleasedCounter.increment();
        availableAssetsGauge.incrementAndGet();
        assetsInUseGauge.decrementAndGet();
    }

    public void incrementNetworkElementCreated() {
        networkElementCreatedCounter.increment();
    }

    public void incrementNetworkElementHeartbeat() {
        networkElementHeartbeatCounter.increment();
    }

    public void incrementSIMCardCreated() {
        simCardCreatedCounter.increment();
        totalSIMCardsGauge.incrementAndGet();
        availableSIMCardsGauge.incrementAndGet();
    }

    public void incrementSIMCardAssigned() {
        simCardAssignedCounter.increment();
        availableSIMCardsGauge.decrementAndGet();
    }

    public Timer.Sample startAssetOperation() {
        return Timer.start();
    }

    public void recordAssetOperation(Timer.Sample sample) {
        sample.stop(assetOperationTimer);
    }

    // Asset gauge update methods
    public void setTotalAssets(long count) {
        totalAssetsGauge.set(count);
    }

    public void setAvailableAssets(long count) {
        availableAssetsGauge.set(count);
    }

    public void setAssetsInUse(long count) {
        assetsInUseGauge.set(count);
    }

    public void setTotalSIMCards(long count) {
        totalSIMCardsGauge.set(count);
    }

    public void setAvailableSIMCards(long count) {
        availableSIMCardsGauge.set(count);
    }

    public void setNetworkElementsOnline(long count) {
        networkElementsOnlineGauge.set(count);
    }
}
