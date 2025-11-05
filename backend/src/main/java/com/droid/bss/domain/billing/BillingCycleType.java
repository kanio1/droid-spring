package com.droid.bss.domain.billing;

/**
 * Types of billing cycles
 */
public enum BillingCycleType {
    MONTHLY("Monthly billing cycle"),
    QUARTERLY("Quarterly billing cycle"),
    YEARLY("Yearly billing cycle"),
    WEEKLY("Weekly billing cycle"),
    ON_DEMAND("On-demand billing");

    private final String description;

    BillingCycleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
