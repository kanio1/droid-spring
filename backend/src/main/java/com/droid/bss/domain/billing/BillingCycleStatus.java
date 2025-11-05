package com.droid.bss.domain.billing;

/**
 * Status of billing cycle
 */
public enum BillingCycleStatus {
    PENDING("Pending - Cycle is scheduled"),
    GENERATED("Generated - Invoices have been generated"),
    PROCESSED("Processed - Cycle is complete"),
    FAILED("Failed - Error during processing");

    private final String description;

    BillingCycleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
