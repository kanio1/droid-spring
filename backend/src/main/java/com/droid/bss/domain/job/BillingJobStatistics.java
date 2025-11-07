package com.droid.bss.domain.job;

import lombok.Builder;
import lombok.Data;

/**
 * Billing Job Statistics
 * Provides summary statistics for billing-related background jobs
 */
@Data
@Builder
public class BillingJobStatistics {

    private long totalBillingJobs;
    private long activeBillingJobs;
    private long invoiceGenerationJobs;
    private long paymentProcessingJobs;
    private long subscriptionJobs;
    private long usageAggregationJobs;

    public long getInactiveBillingJobs() {
        return totalBillingJobs - activeBillingJobs;
    }

    public double getActiveJobPercentage() {
        if (totalBillingJobs == 0) return 0.0;
        return Math.round((double) activeBillingJobs / totalBillingJobs * 10000.0) / 100.0;
    }

    public String getSummary() {
        return String.format("Billing Jobs: %d total, %d active (%.2f%%) | " +
                        "Invoices: %d, Payments: %d, Subscriptions: %d, Usage: %d",
                totalBillingJobs, activeBillingJobs, getActiveJobPercentage(),
                invoiceGenerationJobs, paymentProcessingJobs,
                subscriptionJobs, usageAggregationJobs);
    }
}
