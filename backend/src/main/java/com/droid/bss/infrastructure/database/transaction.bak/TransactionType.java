package com.droid.bss.infrastructure.database.transaction;

/**
 * Types of transactions.
 *
 * @since 1.0
 */
public enum TransactionType {

    /**
     * Local transaction (single resource).
     */
    LOCAL("LOCAL"),

    /**
     * Distributed transaction (multiple resources).
     */
    DISTRIBUTED("DISTRIBUTED"),

    /**
     * XA transaction (distributed with XA protocol).
     */
    XA("XA");

    private final String name;

    TransactionType(String name) {
        this.name = name;
    }

    /**
     * Gets the transaction type name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this is a distributed transaction.
     *
     * @return true if distributed
     */
    public boolean isDistributed() {
        return this == DISTRIBUTED || this == XA;
    }

    /**
     * Checks if this is an XA transaction.
     *
     * @return true if XA
     */
    public boolean isXA() {
        return this == XA;
    }

    /**
     * Checks if this is a local transaction.
     *
     * @return true if local
     */
    public boolean isLocal() {
        return this == LOCAL;
    }

    @Override
    public String toString() {
        return name;
    }
}
