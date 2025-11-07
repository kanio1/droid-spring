package com.droid.bss.domain.invoice;

/**
 * Invoice item type enumeration
 */
public enum InvoiceItemType {
    SUBSCRIPTION("Recurring subscription service"),
    USAGE("Usage-based charge"),
    DISCOUNT("Discount applied"),
    ADJUSTMENT("Invoice adjustment"),
    TAX("Tax charge"),
    ADVANCE_PAYMENT("Advance payment/deposit");

    private final String description;

    InvoiceItemType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAdvancePayment() {
        return this == ADVANCE_PAYMENT;
    }
}
