package com.droid.bss.domain.workforce;

/**
 * Employee status enumeration
 */
public enum EmployeeStatus {
    ACTIVE("Active employee"),
    INACTIVE("Inactive - not currently working"),
    ON_LEAVE("On vacation or leave"),
    SICK("Sick leave"),
    SUSPENDED("Temporarily suspended"),
    TERMINATED("No longer employed"),
    PENDING("Pending activation");

    private final String description;

    EmployeeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canAcceptWork() {
        return this == ACTIVE;
    }
}
