package com.droid.bss.domain.billing;

/**
 * Types of call destinations
 */
public enum DestinationType {
    NATIONAL("National calls"),
    INTERNATIONAL("International calls"),
    MOBILE("Mobile network"),
    FIXED("Fixed line"),
    SPECIAL("Special numbers");

    private final String description;

    DestinationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
