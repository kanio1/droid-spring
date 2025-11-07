package com.droid.bss.domain.order;

import java.util.Objects;
import java.util.UUID;

/**
 * OrderId value object
 * Uniquely identifies an Order aggregate
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("OrderId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique OrderId
     */
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    /**
     * Creates OrderId from UUID string
     */
    public static OrderId of(String uuid) {
        return new OrderId(UUID.fromString(uuid));
    }

    /**
     * Creates OrderId from UUID object
     */
    public static OrderId of(UUID uuid) {
        return new OrderId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
