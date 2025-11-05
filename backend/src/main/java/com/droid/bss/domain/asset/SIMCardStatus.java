package com.droid.bss.domain.asset;

/**
 * Status of SIM card
 */
public enum SIMCardStatus {
    AVAILABLE("Available - Ready for assignment"),
    ASSIGNED("Assigned - Active in service"),
    SUSPENDED("Suspended - Temporarily disabled"),
    DEACTIVATED("Deactivated - No longer in service"),
    LOST("Lost - Reported as lost"),
    DAMAGED("Damaged - Defective card"),
    TESTING("Testing - Under testing");

    private final String description;

    SIMCardStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
