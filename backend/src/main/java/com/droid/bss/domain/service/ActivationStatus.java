package com.droid.bss.domain.service;

/**
 * Status of a service activation
 */
public enum ActivationStatus {
    PENDING("Pending - Activation is queued"),
    SCHEDULED("Scheduled - Activation is scheduled for future"),
    PROVISIONING("Provisioning - Service is being activated"),
    ACTIVE("Active - Service is active and running"),
    FAILED("Failed - Activation failed"),
    DEPROVISIONING("Deprovisioning - Service is being deactivated"),
    INACTIVE("Inactive - Service is inactive"),
    CANCELLED("Cancelled - Activation was cancelled");

    private final String description;

    ActivationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
