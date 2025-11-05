package com.droid.bss.infrastructure.database.sharding;

import java.util.Objects;

/**
 * Represents a shard key for determining which shard to use.
 *
 * @since 1.0
 */
public class ShardKey {

    private final String value;
    private final ShardKeyType type;
    private final Object originalKey;

    /**
     * Creates a new ShardKey from a string.
     *
     * @param value the string value
     * @return the shard key
     */
    public static ShardKey of(String value) {
        return new ShardKey(value, ShardKeyType.STRING, value);
    }

    /**
     * Creates a new ShardKey from a long.
     *
     * @param value the long value
     * @return the shard key
     */
    public static ShardKey of(long value) {
        return new ShardKey(String.valueOf(value), ShardKeyType.LONG, value);
    }

    /**
     * Creates a new ShardKey from an integer.
     *
     * @param value the integer value
     * @return the shard key
     */
    public static ShardKey of(int value) {
        return new ShardKey(String.valueOf(value), ShardKeyType.INT, value);
    }

    /**
     * Creates a new ShardKey with custom type.
     *
     * @param value the string value
     * @param type the key type
     * @param originalKey the original key object
     * @return the shard key
     */
    public static ShardKey of(String value, ShardKeyType type, Object originalKey) {
        return new ShardKey(value, type, originalKey);
    }

    /**
     * Creates a new ShardKey from an object.
     *
     * @param key the key object
     * @return the shard key
     */
    public static ShardKey ofObject(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        if (key instanceof String) {
            return of((String) key);
        } else if (key instanceof Long || key instanceof Integer) {
            return of(key.toString());
        } else {
            // For other types, use toString()
            return of(key.toString());
        }
    }

    private ShardKey(String value, ShardKeyType type, Object originalKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Shard key value cannot be null or blank");
        }
        this.value = value;
        this.type = type;
        this.originalKey = originalKey;
    }

    /**
     * Gets the string value of the key.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the key type.
     *
     * @return the type
     */
    public ShardKeyType getType() {
        return type;
    }

    /**
     * Gets the original key object.
     *
     * @return the original key (may be null)
     */
    public Object getOriginalKey() {
        return originalKey;
    }

    /**
     * Gets the long value of the key.
     *
     * @return the long value
     * @throws NumberFormatException if the key cannot be parsed as a long
     */
    public long getLongValue() {
        return Long.parseLong(value);
    }

    /**
     * Gets the integer value of the key.
     *
     * @return the integer value
     * @throws NumberFormatException if the key cannot be parsed as an integer
     */
    public int getIntValue() {
        return Integer.parseInt(value);
    }

    /**
     * Checks if this key is of the given type.
     *
     * @param type the type to check
     * @return true if the key is of this type
     */
    public boolean isOfType(ShardKeyType type) {
        return this.type == type;
    }

    /**
     * Checks if this key is numeric.
     *
     * @return true if numeric
     */
    public boolean isNumeric() {
        return type == ShardKeyType.LONG || type == ShardKeyType.INT;
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
            "value='" + value + '\'' +
            ", type=" + type +
            '}';
    }
}
