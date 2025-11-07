package com.droid.bss.domain.partner;

/**
 * Partner type enumeration
 */
public enum PartnerType {
    RESELLER("Reseller - Resells services to end customers"),
    DISTRIBUTOR("Distributor - Distributes services to resellers"),
    MVNO("MVNO - Mobile Virtual Network Operator"),
    FRANCHISEE("Franchisee - Operates under brand license"),
    AGENT("Agent - Represents company in sales"),
    SYSTEM_INTEGRATOR("System Integrator - Integrates services for clients"),
    VALUE_ADDED_RESELLER("VAR - Adds value to resold services");

    private final String description;

    PartnerType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReseller() {
        return this == RESELLER;
    }

    public boolean isDistributor() {
        return this == DISTRIBUTOR;
    }

    public boolean isMvno() {
        return this == MVNO;
    }

    public boolean requiresCommission() {
        return this == RESELLER || this == DISTRIBUTOR || this == MVNO || this == AGENT;
    }
}
