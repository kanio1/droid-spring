package com.droid.bss.application.service;

import com.droid.bss.domain.job.BackgroundJob;
import com.droid.bss.infrastructure.performance.JobSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Billing Job Service
 * Manages scheduled jobs for billing cycles, invoice generation, and payment processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingJobService {

    private final JobSchedulerService jobSchedulerService;

    /**
     * Set up all standard billing jobs
     * Call this during application startup or when billing features are enabled
     */
    @Transactional
    public List<BackgroundJob> setupBillingJobs() {
        log.info("Setting up billing jobs...");

        List<BackgroundJob> createdJobs = new ArrayList<>();

        try {
            // 1. Daily usage data aggregation
            BackgroundJob dailyAggregation = jobSchedulerService.scheduleJob(
                    "Daily Usage Aggregation",
                    "Aggregate usage data from previous day",
                    "0 1 * * *", // Daily at 1 AM
                    "SELECT * FROM aggregate_daily_usage();",
                    "HIGH"
            );
            createdJobs.add(dailyAggregation);

            // 2. Monthly billing cycle - generate invoices
            BackgroundJob monthlyInvoices = jobSchedulerService.scheduleJob(
                    "Monthly Invoice Generation",
                    "Generate invoices for active subscriptions",
                    "0 2 1 * *", // 1st day of month at 2 AM
                    "SELECT * FROM generate_monthly_invoices(CURRENT_DATE - INTERVAL '1 month');",
                    "HIGH"
            );
            createdJobs.add(monthlyInvoices);

            // 3. Send invoice notifications
            BackgroundJob invoiceNotifications = jobSchedulerService.scheduleJob(
                    "Invoice Notification Dispatch",
                    "Send email notifications for new invoices",
                    "0 3 1 * *", // 1st day of month at 3 AM
                    "SELECT * FROM send_invoice_notifications();",
                    "MEDIUM"
            );
            createdJobs.add(invoiceNotifications);

            // 4. Process payments
            BackgroundJob paymentProcessing = jobSchedulerService.scheduleJob(
                    "Payment Processing",
                    "Process pending payments and update invoice status",
                    "0 4 * * *", // Daily at 4 AM
                    "SELECT * FROM process_pending_payments();",
                    "HIGH"
            );
            createdJobs.add(paymentProcessing);

            // 5. Weekly subscription renewals check
            BackgroundJob renewalCheck = jobSchedulerService.scheduleJob(
                    "Subscription Renewal Check",
                    "Check for upcoming subscription renewals",
                    "0 5 * * 1", // Weekly on Monday at 5 AM
                    "SELECT * FROM check_subscription_renewals();",
                    "MEDIUM"
            );
            createdJobs.add(renewalCheck);

            // 6. Monthly subscription renewals
            BackgroundJob subscriptionRenewals = jobSchedulerService.scheduleJob(
                    "Monthly Subscription Renewals",
                    "Process subscription renewals for the month",
                    "0 6 1 * *", // 1st day of month at 6 AM
                    "SELECT * FROM process_subscription_renewals();",
                    "HIGH"
            );
            createdJobs.add(subscriptionRenewals);

            // 7. Quarterly financial reports
            BackgroundJob quarterlyReports = jobSchedulerService.scheduleJob(
                    "Quarterly Financial Reports",
                    "Generate quarterly financial reports",
                    "0 7 1 1,4,7,10 *", // 1st day of quarter at 7 AM
                    "SELECT * FROM generate_quarterly_reports();",
                    "LOW"
            );
            createdJobs.add(quarterlyReports);

            // 8. Weekly payment retry
            BackgroundJob paymentRetry = jobSchedulerService.scheduleJob(
                    "Failed Payment Retry",
                    "Retry failed payments from the past week",
                    "0 8 * * 1", // Weekly on Monday at 8 AM
                    "SELECT * FROM retry_failed_payments();",
                    "MEDIUM"
            );
            createdJobs.add(paymentRetry);

            // 9. Monthly usage-based billing calculation
            BackgroundJob usageBilling = jobSchedulerService.scheduleJob(
                    "Usage-Based Billing Calculation",
                    "Calculate and invoice usage-based charges",
                    "0 9 1 * *", // 1st day of month at 9 AM
                    "SELECT * FROM calculate_usage_billing();",
                    "HIGH"
            );
            createdJobs.add(usageBilling);

            // 10. Daily subscription status cleanup
            BackgroundJob statusCleanup = jobSchedulerService.scheduleJob(
                    "Subscription Status Cleanup",
                    "Clean up expired and cancelled subscriptions",
                    "0 0 * * *", // Daily at midnight
                    "SELECT * FROM cleanup_subscription_status();",
                    "LOW"
            );
            createdJobs.add(statusCleanup);

            log.info("Successfully created {} billing jobs", createdJobs.size());
            return createdJobs;

        } catch (Exception e) {
            log.error("Failed to set up billing jobs", e);
            // Return partial list if some jobs were created
            if (!createdJobs.isEmpty()) {
                log.warn("Partial success: {} billing jobs were created before error", createdJobs.size());
                return createdJobs;
            }
            throw new RuntimeException("Failed to set up billing jobs", e);
        }
    }

    /**
     * Get billing job statistics
     */
    public BillingJobStatistics getBillingJobStatistics() {
        List<BackgroundJob> allJobs = jobSchedulerService.getAllJobs();

        long totalBillingJobs = allJobs.size();
        long activeBillingJobs = allJobs.stream()
                .mapToLong(j -> "ACTIVE".equals(j.getStatus()) ? 1 : 0)
                .sum();

        // Categorize jobs
        long invoiceJobs = allJobs.stream()
                .mapToLong(j -> j.getName().toLowerCase().contains("invoice") ? 1 : 0)
                .sum();

        long paymentJobs = allJobs.stream()
                .mapToLong(j -> j.getName().toLowerCase().contains("payment") ? 1 : 0)
                .sum();

        long subscriptionJobs = allJobs.stream()
                .mapToLong(j -> j.getName().toLowerCase().contains("subscription") ? 1 : 0)
                .sum();

        long usageJobs = allJobs.stream()
                .mapToLong(j -> j.getName().toLowerCase().contains("usage") ? 1 : 0)
                .sum();

        return BillingJobStatistics.builder()
                .totalBillingJobs(totalBillingJobs)
                .activeBillingJobs(activeBillingJobs)
                .invoiceGenerationJobs(invoiceJobs)
                .paymentProcessingJobs(paymentJobs)
                .subscriptionJobs(subscriptionJobs)
                .usageAggregationJobs(usageJobs)
                .build();
    }

    /**
     * Schedule a custom billing job
     */
    @Transactional
    public BackgroundJob scheduleCustomBillingJob(String name, String description,
                                                   String cronExpression, String sqlCommand,
                                                   String priority) {
        log.info("Scheduling custom billing job: {}", name);

        BackgroundJob job = jobSchedulerService.scheduleJob(
                name,
                cronExpression,
                sqlCommand,
                description,
                priority != null ? priority : "MEDIUM"
        );

        return job;
    }

    /**
     * Remove all billing jobs
     */
    @Transactional
    public void removeAllBillingJobs() {
        log.warn("Removing all billing jobs...");

        List<BackgroundJob> allJobs = jobSchedulerService.getAllJobs();
        long removedCount = 0;

        for (BackgroundJob job : allJobs) {
            if (isBillingRelated(job)) {
                try {
                    jobSchedulerService.cancelJob(job.getId());
                    removedCount++;
                } catch (Exception e) {
                    log.error("Failed to remove job: {}", job.getName(), e);
                }
            }
        }

        log.info("Removed {} billing jobs", removedCount);
    }

    /**
     * Check if a job is billing-related
     */
    private boolean isBillingRelated(BackgroundJob job) {
        String name = job.getName().toLowerCase();
        return name.contains("invoice") ||
               name.contains("payment") ||
               name.contains("billing") ||
               name.contains("subscription") ||
               name.contains("usage") ||
               name.contains("renewal");
    }
}
