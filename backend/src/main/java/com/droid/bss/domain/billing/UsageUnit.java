package com.droid.bss.domain.billing;

/**
 * Units of usage measurement
 */
public enum UsageUnit {
    SECONDS("Time in seconds"),
    MINUTES("Time in minutes"),
    COUNT("Number of units"),
    MB("Megabytes"),
    GB("Gigabytes");

    private final String description;

    UsageUnit(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
