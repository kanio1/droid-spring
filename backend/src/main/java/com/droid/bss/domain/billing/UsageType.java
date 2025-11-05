package com.droid.bss.domain.billing;

/**
 * Types of usage for billing
 */
public enum UsageType {
    VOICE("Voice calls"),
    SMS("SMS messages"),
    DATA("Data transfer"),
    VIDEO("Video calls"),
    ROAMING("Roaming usage");

    private final String description;

    UsageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
