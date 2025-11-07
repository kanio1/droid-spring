package com.droid.bss.domain.payment;

/**
 * Payment reconciliation item status enumeration
 */
public enum ReconciliationItemStatus {
    UNMATCHED("Unmatched - No bank record found"),
    MATCHED("Matched - Successfully reconciled"),
    DISCREPANCY("Discrepancy - Amount/date mismatch"),
    RESOLVED("Resolved - Discrepancy has been resolved");

    private final String description;

    ReconciliationItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMatched() {
        return this == MATCHED;
    }

    public boolean isUnmatched() {
        return this == UNMATCHED;
    }

    public boolean hasDiscrepancy() {
        return this == DISCREPANCY;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }
}
