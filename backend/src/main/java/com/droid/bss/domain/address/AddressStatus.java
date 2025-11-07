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

    public boolean canTransitionTo(AddressStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus == INACTIVE || newStatus == PENDING;
            case INACTIVE -> newStatus == ACTIVE;
            case PENDING -> newStatus == ACTIVE || newStatus == INACTIVE;
        };
    }
}
