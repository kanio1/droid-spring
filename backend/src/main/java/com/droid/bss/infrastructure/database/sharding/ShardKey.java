package com.droid.bss.infrastructure.database.sharding;

import java.util.Objects;
import java.util.UUID;

/**
 * Stub class for shard key
 * Minimal implementation for testing purposes
 */
public class ShardKey {

    private final UUID value;
    private final String stringValue;

    public ShardKey(String value) {
        this.value = UUID.randomUUID();
        this.stringValue = value;
    }

    public ShardKey(UUID value) {
        this.value = value;
        this.stringValue = value.toString();
    }

    public UUID getValue() {
        return value;
    }

    public String getStringValue() {
        return stringValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShardKey shardKey = (ShardKey) o;
        return Objects.equals(value, shardKey.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ShardKey{" +
                "value=" + value +
                ", stringValue='" + stringValue + '\'' +
                '}';
    }
}
