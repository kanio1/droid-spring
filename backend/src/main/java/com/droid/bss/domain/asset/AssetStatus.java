package com.droid.bss.domain.asset;

/**
 * Status of an asset in the inventory
 */
public enum AssetStatus {
    AVAILABLE("Available - Not assigned, ready for use"),
    IN_USE("In Use - Currently assigned to customer/location"),
    RESERVED("Reserved - Held for specific use"),
    MAINTENANCE("Maintenance - Under repair or maintenance"),
    DAMAGED("Damaged - Defective, needs repair"),
    STOLEN("Stolen - Reported as stolen"),
    RETIRED("Retired - Decommissioned, not in use"),
    LOST("Lost - Lost or misplaced"),
    RETURNED("Returned - Returned to inventory");

    private final String description;

    AssetStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
