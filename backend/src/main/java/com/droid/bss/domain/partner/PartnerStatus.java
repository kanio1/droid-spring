package com.droid.bss.domain.partner;

/**
 * Partner status enumeration
 */
public enum PartnerStatus {
    ACTIVE("Active - Can process orders and transactions"),
    SUSPENDED("Suspended - Temporarily inactive"),
    TERMINATED("Terminated - Contract ended"),
    PENDING_APPROVAL("Pending Approval - Awaiting approval"),
    ON_HOLD("On Hold - Temporarily paused");

    private final String description;

    PartnerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canProcessOrders() {
        return this == ACTIVE;
    }
}
