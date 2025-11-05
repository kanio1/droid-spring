package com.droid.bss.infrastructure.database.sharding;

/**
 * Types of shard operations.
 *
 * @since 1.0
 */
public enum ShardOperation {

    /**
     * Read operation.
     */
    READ("READ"),

    /**
     * Write operation.
     */
    WRITE("WRITE"),

    /**
     * Read-write operation.
     */
    READ_WRITE("READ_WRITE"),

    /**
     * Delete operation.
     */
    DELETE("DELETE"),

    /**
     * Update operation.
     */
    UPDATE("UPDATE"),

    /**
     * Batch operation.
     */
    BATCH("BATCH");

    private final String name;

    ShardOperation(String name) {
        this.name = name;
    }

    /**
     * Gets the operation name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this is a read operation.
     *
     * @return true if read
     */
    public boolean isRead() {
        return this == READ || this == READ_WRITE;
    }

    /**
     * Checks if this is a write operation.
     *
     * @return true if write
     */
    public boolean isWrite() {
        return this == WRITE || this == READ_WRITE || this == UPDATE || this == DELETE;
    }

    /**
     * Checks if this is a read-write operation.
     *
     * @return true if read-write
     */
    public boolean isReadWrite() {
        return this == READ_WRITE;
    }

    /**
     * Checks if this is a delete operation.
     *
     * @return true if delete
     */
    public boolean isDelete() {
        return this == DELETE;
    }

    /**
     * Checks if this is an update operation.
     *
     * @return true if update
     */
    public boolean isUpdate() {
        return this == UPDATE;
    }

    /**
     * Checks if this is a batch operation.
     *
     * @return true if batch
     */
    public boolean isBatch() {
        return this == BATCH;
    }

    @Override
    public String toString() {
        return name;
    }
}
