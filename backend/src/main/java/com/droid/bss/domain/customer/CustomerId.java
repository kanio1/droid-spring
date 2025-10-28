package com.droid.bss.domain.customer;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {
    
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId value cannot be null");
        if (value.toString().equals("00000000-0000-0000-0000-000000000000")) {
            throw new IllegalArgumentException("CustomerId cannot be zero UUID");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }
    
    public static CustomerId of(String uuid) {
        return new CustomerId(UUID.fromString(uuid));
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
