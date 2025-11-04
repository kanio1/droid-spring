package com.droid.bss.domain.product;

/**
 * Product type enumeration
 */
public enum ProductType {
    SERVICE("Usługa"),
    TARIFF("Taryfa"),
    BUNDLE("Pakiet"),
    ADDON("Dodatek");

    private final String displayName;

    ProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
