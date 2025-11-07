package com.droid.bss.domain.workforce;

/**
 * Time sheet status enumeration
 */
public enum TimeSheetStatus {
    IN_PROGRESS("Employee is currently working"),
    COMPLETED("Work completed, waiting for approval"),
    PENDING_APPROVAL("Submitted for approval"),
    APPROVED("Approved by supervisor"),
    REJECTED("Rejected by supervisor"),
    PAID("Payment processed");

    private final String description;

    TimeSheetStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEditable() {
        return this == IN_PROGRESS || this == COMPLETED;
    }

    public boolean isFinal() {
        return this == APPROVED || this == PAID || this == REJECTED;
    }
}
