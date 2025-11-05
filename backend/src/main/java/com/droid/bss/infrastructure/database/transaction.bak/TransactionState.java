package com.droid.bss.infrastructure.database.transaction;

/**
 * States of a transaction.
 *
 * @since 1.0
 */
public enum TransactionState {

    /**
     * Transaction is active.
     */
    ACTIVE("ACTIVE"),

    /**
     * Transaction is marked for rollback only.
     */
    MARKED_ROLLBACK("MARKED_ROLLBACK"),

    /**
     * Transaction has been prepared (2PC).
     */
    PREPARED("PREPARED"),

    /**
     * Transaction has been committed.
     */
    COMMITTED("COMMITTED"),

    /**
     * Transaction has been rolled back.
     */
    ROLLED_BACK("ROLLED_BACK"),

    /**
     * Transaction is suspending (being moved to another thread).
     */
    SUSPENDING("SUSPENDING"),

    /**
     * Transaction has been suspended.
     */
    SUSPENDED("SUSPENDED"),

    /**
     * Transaction is resuming.
     */
    RESUMING("RESUMING"),

    /**
     * Transaction is being committed.
     */
    COMMITTING("COMMITTING"),

    /**
     * Transaction is being rolled back.
     */
    ROLLING_BACK("ROLLING_BACK");

    private final String name;

    TransactionState(String name) {
        this.name = name;
    }

    /**
     * Gets the state name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this is an active state.
     *
     * @return true if active
     */
    public boolean isActive() {
        return this == ACTIVE || this == SUSPENDING || this == SUSPENDED || this == PREPARED;
    }

    /**
     * Checks if this is a completed state.
     *
     * @return true if completed
     */
    public boolean isCompleted() {
        return this == COMMITTED || this == ROLLED_BACK;
    }

    /**
     * Checks if this is a suspended state.
     *
     * @return true if suspended
     */
    public boolean isSuspended() {
        return this == SUSPENDING || this == SUSPENDED || this == RESUMING;
    }

    /**
     * Checks if this is a transient state (intermediate state).
     *
     * @return true if transient
     */
    public boolean isTransient() {
        return this == SUSPENDING || this == RESUMING || this == COMMITTING || this == ROLLING_BACK;
    }

    @Override
    public String toString() {
        return name;
    }
}
