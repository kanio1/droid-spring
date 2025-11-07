package com.droid.bss.domain.address;

import java.util.Objects;
import java.util.UUID;

/**
 * AddressId value object
 * Uniquely identifies an Address aggregate
 */
public record AddressId(UUID value) {

    public AddressId {
        Objects.requireNonNull(value, "AddressId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("AddressId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique AddressId
     */
    public static AddressId generate() {
        return new AddressId(UUID.randomUUID());
    }

    /**
     * Creates AddressId from UUID string
     */
    public static AddressId of(String uuid) {
        return new AddressId(UUID.fromString(uuid));
    }

    /**
     * Creates AddressId from UUID object
     */
    public static AddressId of(UUID uuid) {
        return new AddressId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
