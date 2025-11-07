package com.droid.bss.domain.payment;

import java.util.Objects;
import java.util.UUID;

/**
 * PaymentId value object
 * Uniquely identifies a Payment aggregate
 */
public record PaymentId(UUID value) {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("PaymentId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique PaymentId
     */
    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    /**
     * Creates PaymentId from UUID string
     */
    public static PaymentId of(String uuid) {
        return new PaymentId(UUID.fromString(uuid));
    }

    /**
     * Creates PaymentId from UUID object
     */
    public static PaymentId of(UUID uuid) {
        return new PaymentId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
