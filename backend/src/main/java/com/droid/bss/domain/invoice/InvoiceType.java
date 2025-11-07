package com.droid.bss.domain.invoice;

/**
 * Invoice type enumeration
 */
public enum InvoiceType {
    RECURRING("Recurring subscription invoice"),
    ONE_TIME("One-time service invoice"),
    USAGE("Usage-based invoice"),
    ADJUSTMENT("Invoice adjustment"),
    ADVANCE("Advance payment invoice"),
    PRORATED("Prorated invoice (partial period)");

    private final String description;

    InvoiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAdvance() {
        return this == ADVANCE;
    }

    public boolean isProrated() {
        return this == PRORATED;
    }

    public boolean isRecurring() {
        return this == RECURRING;
    }
}
