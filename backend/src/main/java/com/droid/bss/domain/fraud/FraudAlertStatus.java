package com.droid.bss.domain.fraud;

/**
 * Fraud alert status enumeration
 */
public enum FraudAlertStatus {
    NEW("New alert, not yet reviewed"),
    ASSIGNED("Assigned to analyst"),
    IN_REVIEW("Currently under review"),
    ESCALATED("Escalated to higher level"),
    RESOLVED("Resolved - confirmed fraud"),
    CLOSED("Closed - no fraud detected"),
    REJECTED("Alert rejected");

    private final String description;

    FraudAlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOpen() {
        return this == NEW || this == ASSIGNED || this == IN_REVIEW || this == ESCALATED;
    }
}
