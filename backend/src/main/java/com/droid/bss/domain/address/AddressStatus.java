package com.droid.bss.domain.address;

/**
 * Address status enum for managing address lifecycle
 */
public enum AddressStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    PENDING("Pending");

    private final String description;

    AddressStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
