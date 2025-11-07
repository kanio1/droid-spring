package com.droid.bss.domain.product;

import java.util.Objects;
import java.util.UUID;

/**
 * ProductId value object
 * Uniquely identifies a Product aggregate
 */
public record ProductId(UUID value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("ProductId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique ProductId
     */
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    /**
     * Creates ProductId from UUID string
     */
    public static ProductId of(String uuid) {
        return new ProductId(UUID.fromString(uuid));
    }

    /**
     * Creates ProductId from UUID object
     */
    public static ProductId of(UUID uuid) {
        return new ProductId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
