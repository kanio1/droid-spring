package com.droid.bss.domain.subscription;

import java.util.Objects;
import java.util.UUID;

/**
 * SubscriptionId value object
 * Uniquely identifies a Subscription aggregate
 */
public record SubscriptionId(UUID value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "SubscriptionId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("SubscriptionId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique SubscriptionId
     */
    public static SubscriptionId generate() {
        return new SubscriptionId(UUID.randomUUID());
    }

    /**
     * Creates SubscriptionId from UUID string
     */
    public static SubscriptionId of(String uuid) {
        return new SubscriptionId(UUID.fromString(uuid));
    }

    /**
     * Creates SubscriptionId from UUID object
     */
    public static SubscriptionId of(UUID uuid) {
        return new SubscriptionId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
