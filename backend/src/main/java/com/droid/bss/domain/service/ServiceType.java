package com.droid.bss.domain.service;

/**
 * Types of services available in the BSS system
 */
public enum ServiceType {
    INTERNET("Internet"),
    TELEPHONY("Telephony"),
    TELEVISION("Television"),
    MOBILE("Mobile"),
    CLOUD("Cloud Services"),
    SUPPORT("Support Services"),
    ADDON("Add-on Service"),
    BUNDLE("Service Bundle");

    private final String description;

    ServiceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
