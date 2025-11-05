package com.droid.bss.domain.address;

/**
 * Address type enum for categorizing customer addresses
 */
public enum AddressType {
    BILLING("Billing Address"),
    SHIPPING("Shipping Address"),
    SERVICE("Service Address"),
    CORRESPONDENCE("Correspondence Address");

    private final String description;

    AddressType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
