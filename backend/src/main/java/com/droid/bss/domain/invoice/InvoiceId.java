package com.droid.bss.domain.invoice;

import java.util.Objects;
import java.util.UUID;

/**
 * InvoiceId value object
 * Uniquely identifies an Invoice aggregate
 */
public record InvoiceId(UUID value) {

    public InvoiceId {
        Objects.requireNonNull(value, "InvoiceId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("InvoiceId cannot be zero UUID");
        }
    }

    /**
     * Generates a new unique InvoiceId
     */
    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }

    /**
     * Creates InvoiceId from UUID string
     */
    public static InvoiceId of(String uuid) {
        return new InvoiceId(UUID.fromString(uuid));
    }

    /**
     * Creates InvoiceId from UUID object
     */
    public static InvoiceId of(UUID uuid) {
        return new InvoiceId(uuid);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
