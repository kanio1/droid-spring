package com.droid.bss.domain.fraud;

/**
 * Fraud rule type enumeration
 */
public enum FraudRuleType {
    VELOCITY("Velocity - checks transaction frequency"),
    THRESHOLD("Threshold - checks values/amounts"),
    PATTERN("Pattern - detects unusual patterns"),
    GEOGRAPHIC("Geographic - checks location anomalies"),
    DEVICE("Device - device fingerprinting"),
    BEHAVIORAL("Behavioral - user behavior analysis"),
    BLACKLIST("Blacklist - checks against blacklists"),
    WHITELIST("Whitelist - approved entities"),
    MACHINE_LEARNING("Machine learning model"),
    RULE_ENGINE("Custom rule engine"),
    MANUAL_REVIEW("Manual review required");

    private final String description;

    FraudRuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAutomated() {
        return this != MANUAL_REVIEW;
    }
}
