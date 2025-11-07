package com.droid.bss.domain.payment;

/**
 * Payment reconciliation status enumeration
 */
public enum ReconciliationStatus {
    PENDING("Pending - Not started"),
    IN_PROGRESS("In progress - Currently being reconciled"),
    COMPLETED("Completed - Successfully reconciled"),
    DISCREPANCY("Discrepancy - Has unresolved issues");

    private final String description;

    ReconciliationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean hasDiscrepancy() {
        return this == DISCREPANCY;
    }
}
