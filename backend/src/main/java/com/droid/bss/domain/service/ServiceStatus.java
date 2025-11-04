package com.droid.bss.domain.service;

/**
 * Status of a service definition
 */
public enum ServiceStatus {
    ACTIVE("Active - Service is available for activation"),
    INACTIVE("Inactive - Service is temporarily unavailable"),
    DEPRECATED("Deprecated - Service will be discontinued"),
    PLANNED("Planned - Service is being prepared");

    private final String description;

    ServiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
