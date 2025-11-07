package com.droid.bss.domain.user;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a User ID.
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "User ID value cannot be null");
    }

    /**
     * Generates a new random User ID.
     */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Creates a User ID from a UUID string.
     */
    public static UserId from(String uuid) {
        return new UserId(UUID.fromString(uuid));
    }

    /**
     * Creates a User ID from a UUID.
     */
    public static UserId from(UUID uuid) {
        return new UserId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
