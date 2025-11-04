package com.droid.bss.domain.product;

/**
 * Product status enumeration
 */
public enum ProductStatus {
    ACTIVE("Aktywny"),
    INACTIVE("Nieaktywny"),
    DEPRECATED("Wycofany"),
    SUSPENDED("Zawieszony");

    private final String displayName;

    ProductStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
