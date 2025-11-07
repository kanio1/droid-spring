package com.droid.bss.domain.workforce;

/**
 * Work order status enumeration
 */
public enum WorkOrderStatus {
    PENDING("New, not yet assigned"),
    SCHEDULED("Scheduled for specific date/time"),
    ASSIGNED("Assigned to technician"),
    IN_PROGRESS("Technician is working on it"),
    ON_HOLD("Temporarily paused"),
    COMPLETED("Work finished successfully"),
    CANCELLED("Work order cancelled"),
    REQUIRES_REVIEW("Needs supervisor review"),
    REQUIRES_PARTS("Waiting for parts/materials");

    private final String description;

    WorkOrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == PENDING || this == SCHEDULED || this == ASSIGNED || this == IN_PROGRESS || this == ON_HOLD;
    }

    public boolean isCompleted() {
        return this == COMPLETED || this == CANCELLED;
    }
}
