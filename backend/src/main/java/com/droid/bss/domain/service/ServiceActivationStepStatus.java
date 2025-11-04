package com.droid.bss.domain.service;

/**
 * Status of a service activation step
 */
public enum ServiceActivationStepStatus {
    PENDING("Pending - Step is waiting to be executed"),
    IN_PROGRESS("In Progress - Step is currently executing"),
    COMPLETED("Completed - Step executed successfully"),
    FAILED("Failed - Step execution failed"),
    SKIPPED("Skipped - Step was skipped"),
    CANCELLED("Cancelled - Step execution was cancelled");

    private final String description;

    ServiceActivationStepStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
