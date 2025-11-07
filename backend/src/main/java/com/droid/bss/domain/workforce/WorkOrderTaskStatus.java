package com.droid.bss.domain.workforce;

/**
 * Work order task status enumeration
 */
public enum WorkOrderTaskStatus {
    PENDING("Not yet started"),
    IN_PROGRESS("Currently being worked on"),
    COMPLETED("Task finished"),
    SKIPPED("Task was skipped"),
    FAILED("Task failed");

    private final String description;

    WorkOrderTaskStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == COMPLETED || this == SKIPPED;
    }
}
