package com.droid.bss.domain.product;

/**
 * Product category enumeration
 */
public enum ProductCategory {
    MOBILE("Mobilny"),
    BROADBAND("Szerokopasmowy"),
    TV("Telewizja"),
    CLOUD("Chmura"),
    BASIC("Podstawowy");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
