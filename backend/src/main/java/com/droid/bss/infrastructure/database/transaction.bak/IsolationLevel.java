package com.droid.bss.infrastructure.database.transaction;

import java.sql.Connection;

/**
 * Transaction isolation levels.
 *
 * @since 1.0
 */
public enum IsolationLevel {

    /**
     * Use the default isolation level of the database.
     */
    DEFAULT(-1, "DEFAULT"),

    /**
     * No isolation level set (use connection default).
     */
    NONE(Connection.TRANSACTION_NONE, "NONE"),

    /**
     * Read committed isolation level.
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED"),

    /**
     * Read uncommitted isolation level.
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED"),

    /**
     * Repeatable read isolation level.
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ"),

    /**
     * Serializable isolation level.
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE");

    private final int level;
    private final String name;

    IsolationLevel(int level, String name) {
        this.level = level;
        this.name = name;
    }

    /**
     * Gets the JDBC transaction isolation level.
     *
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Gets the isolation level name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the isolation level by name.
     *
     * @param name the name (case-insensitive)
     * @return the isolation level
     * @throws IllegalArgumentException if no matching level is found
     */
    public static IsolationLevel fromName(String name) {
        if (name == null) {
            return DEFAULT;
        }

        for (IsolationLevel level : values()) {
            if (level.name.equalsIgnoreCase(name)) {
                return level;
            }
        }

        throw new IllegalArgumentException("Unknown isolation level: " + name);
    }

    /**
     * Gets the isolation level by JDBC level.
     *
     * @param level the JDBC level
     * @return the isolation level
     * @throws IllegalArgumentException if no matching level is found
     */
    public static IsolationLevel fromLevel(int level) {
        for (IsolationLevel isolationLevel : values()) {
            if (isolationLevel.getLevel() == level) {
                return isolationLevel;
            }
        }

        throw new IllegalArgumentException("Unknown isolation level: " + level);
    }

    @Override
    public String toString() {
        return name;
    }
}
