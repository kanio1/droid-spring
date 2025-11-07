package com.droid.bss.domain.workforce;

/**
 * Employee type enumeration
 */
public enum EmployeeType {
    TECHNICIAN("Field technician - performs installations and repairs"),
    SENIOR_TECHNICIAN("Senior technician with advanced skills"),
    SUPERVISOR("Team supervisor"),
    MANAGER("Department manager"),
    SPECIALIST("Technical specialist"),
    CONTRACTOR("External contractor"),
    DISPATCHER("Dispatch coordinator"),
    INSPECTOR("Quality inspector");

    private final String description;

    EmployeeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFieldWorker() {
        return this == TECHNICIAN || this == SENIOR_TECHNICIAN;
    }

    public boolean isManager() {
        return this == SUPERVISOR || this == MANAGER;
    }
}
