package com.droid.bss.domain.fraud;

/**
 * Fraud alert type enumeration
 */
public enum FraudAlertType {
    UNUSUAL_LOGIN("Unusual login pattern"),
    MULTIPLE_FAILED_LOGINS("Multiple failed login attempts"),
    HIGH_VALUE_TRANSACTION("High value transaction"),
    RAPID_TRANSACTIONS("Rapid sequence of transactions"),
    LOCATION_ANOMALY("Transaction from unusual location"),
    IP_REPUTATION("Suspicious IP address"),
    VELOCITY_CHECK("Velocity rule violation"),
    PATTERN_ANOMALY("Unusual transaction pattern"),
    DEVICE_MISMATCH("Unknown device used"),
    ACCOUNT_TAKEOVER("Potential account takeover"),
    PAYMENT_FRAUD("Suspicious payment activity"),
    IDENTITY_VERIFICATION("Identity verification mismatch"),
    BLACKLISTED_ENTITY("Blacklisted entity detected"),
    CROSS_ACCOUNT_ACTIVITY("Suspicious cross-account activity"),
    MANUAL_REVIEW("Manual review required");

    private final String description;

    FraudAlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresImmediateAction() {
        return this == ACCOUNT_TAKEOVER || this == HIGH_VALUE_TRANSACTION || this == IDENTITY_VERIFICATION;
    }
}
