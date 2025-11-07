package com.droid.bss.domain.workforce;

/**
 * Schedule status enumeration
 */
public enum ScheduleStatus {
    SCHEDULED("Planned but not confirmed"),
    CONFIRMED("Employee has confirmed"),
    IN_PROGRESS("Currently working"),
    COMPLETED("Shift completed"),
    CANCELLED("Shift cancelled"),
    NO_SHOW("Employee did not show up"),
    PARTIAL("Partially worked");

    private final String description;

    ScheduleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == SCHEDULED || this == CONFIRMED || this == IN_PROGRESS;
    }
}
