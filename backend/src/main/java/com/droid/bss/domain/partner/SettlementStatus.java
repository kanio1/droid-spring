package com.droid.bss.domain.partner;

/**
 * Settlement status enumeration
 */
public enum SettlementStatus {
    PENDING("Pending - Awaiting processing"),
    PROCESSED("Processed - Ready for payment"),
    PAID("Paid - Settlement completed"),
    REVERSED("Reversed - Settlement reversed"),
    CANCELLED("Cancelled - Settlement cancelled");

    private final String description;

    SettlementStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean canBeProcessed() {
        return this == PENDING;
    }
}
