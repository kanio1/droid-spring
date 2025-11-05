package com.droid.bss.domain.billing;

/**
 * Rate periods for time-based pricing
 */
public enum RatePeriod {
    PEAK("Peak hours - higher rates"),
    OFF_PEAK("Off-peak hours - lower rates"),
    WEEKEND("Weekend rates");

    private final String description;

    RatePeriod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
