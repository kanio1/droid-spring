package com.droid.bss.domain.workforce;

/**
 * Schedule type enumeration
 */
public enum ScheduleType {
    REGULAR("Regular work shift"),
    OVERTIME("Overtime shift"),
    ON_CALL("On-call availability"),
    WEEKEND("Weekend shift"),
    HOLIDAY("Holiday shift"),
    NIGHT("Night shift"),
    SPLIT("Split shift"),
    FLEXIBLE("Flexible schedule"),
    REMOTE("Remote work");

    private final String description;

    ScheduleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBillable() {
        return this == REGULAR || this == OVERTIME || this == WEEKEND || this == HOLIDAY || this == NIGHT;
    }
}
