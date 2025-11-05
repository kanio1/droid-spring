package com.droid.bss.infrastructure.database.sharding;

/**
 * Types of shard keys.
 *
 * @since 1.0
 */
public enum ShardKeyType {

    /**
     * String-based shard key.
     */
    STRING("STRING"),

    /**
     * Long-based shard key.
     */
    LONG("LONG"),

    /**
     * Integer-based shard key.
     */
    INT("INT"),

    /**
     * UUID-based shard key.
     */
    UUID("UUID"),

    /**
     * Composite shard key (multiple fields).
     */
    COMPOSITE("COMPOSITE");

    private final String name;

    ShardKeyType(String name) {
        this.name = name;
    }

    /**
     * Gets the type name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Parses a type from string.
     *
     * @param name the type name (case-insensitive)
     * @return the type
     * @throws IllegalArgumentException if no matching type found
     */
    public static ShardKeyType fromString(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Type name cannot be null");
        }

        for (ShardKeyType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown shard key type: " + name);
    }

    /**
     * Checks if this is a numeric type.
     *
     * @return true if numeric
     */
    public boolean isNumeric() {
        return this == LONG || this == INT;
    }

    /**
     * Checks if this is a composite type.
     *
     * @return true if composite
     */
    public boolean isComposite() {
        return this == COMPOSITE;
    }

    @Override
    public String toString() {
        return name;
    }
}
