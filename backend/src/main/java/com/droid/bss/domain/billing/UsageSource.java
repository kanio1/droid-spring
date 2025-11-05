package com.droid.bss.domain.billing;

/**
 * Source of usage records
 */
public enum UsageSource {
    CDR("Call Detail Record"),
    MANUAL("Manual entry"),
    BULK_UPLOAD("Bulk upload");

    private final String description;

    UsageSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
