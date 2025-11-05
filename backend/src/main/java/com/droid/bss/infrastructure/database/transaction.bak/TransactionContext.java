package com.droid.bss.infrastructure.database.transaction;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a transaction context.
 *
 * @since 1.0
 */
public class TransactionContext {

    private final String transactionId;
    private final TransactionType transactionType;
    private final IsolationLevel isolationLevel;
    private final Instant createdAt;
    private final String participantId;

    private TransactionState state;
    private boolean suspended;
    private volatile long timeoutSeconds;

    /**
     * Creates a new TransactionContext.
     *
     * @param transactionType the transaction type
     * @param isolationLevel the isolation level
     * @param participantId the participant ID
     */
    public TransactionContext(TransactionType transactionType, IsolationLevel isolationLevel, String participantId) {
        this.transactionId = UUID.randomUUID().toString();
        this.transactionType = transactionType;
        this.isolationLevel = isolationLevel;
        this.createdAt = Instant.now();
        this.participantId = participantId;
        this.state = TransactionState.ACTIVE;
        this.suspended = false;
        this.timeoutSeconds = 300; // Default 5 minutes
    }

    /**
     * Creates a new TransactionContext with default isolation level.
     *
     * @param transactionType the transaction type
     * @param participantId the participant ID
     */
    public TransactionContext(TransactionType transactionType, String participantId) {
        this(transactionType, IsolationLevel.DEFAULT, participantId);
    }

    /**
     * Creates a new TransactionContext for local transaction.
     *
     * @param participantId the participant ID
     */
    public TransactionContext(String participantId) {
        this(TransactionType.LOCAL, participantId);
    }

    /**
     * Gets the transaction ID.
     *
     * @return the transaction ID
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the transaction type.
     *
     * @return the transaction type
     */
    public TransactionType getTransactionType() {
        return transactionType;
    }

    /**
     * Gets the isolation level.
     *
     * @return the isolation level
     */
    public IsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    /**
     * Gets the creation time.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the participant ID.
     *
     * @return the participant ID
     */
    public String getParticipantId() {
        return participantId;
    }

    /**
     * Gets the transaction state.
     *
     * @return the state
     */
    public TransactionState getState() {
        return state;
    }

    /**
     * Sets the transaction state.
     *
     * @param state the state
     */
    public void setState(TransactionState state) {
        this.state = state;
    }

    /**
     * Checks if the transaction is suspended.
     *
     * @return true if suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets the suspended flag.
     *
     * @param suspended the suspended flag
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Gets the timeout in seconds.
     *
     * @return the timeout
     */
    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Sets the timeout in seconds.
     *
     * @param timeoutSeconds the timeout
     */
    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Checks if the transaction is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return state == TransactionState.ACTIVE && !suspended;
    }

    /**
     * Checks if the transaction is marked for rollback.
     *
     * @return true if marked for rollback
     */
    public boolean isRollbackOnly() {
        return state == TransactionState.MARKED_ROLLBACK;
    }

    /**
     * Marks the transaction for rollback.
     */
    public void setRollbackOnly() {
        state = TransactionState.MARKED_ROLLBACK;
    }

    /**
     * Checks if the transaction has timed out.
     *
     * @return true if timed out
     */
    public boolean isTimeout() {
        long elapsed = java.time.Duration.between(createdAt, Instant.now()).getSeconds();
        return elapsed > timeoutSeconds;
    }

    /**
     * Gets the transaction age in seconds.
     *
     * @return the age in seconds
     */
    public long getAgeSeconds() {
        return java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    }

    /**
     * Checks if this context is for the same transaction.
     *
     * @param other the other context
     * @return true if same transaction
     */
    public boolean isSameTransaction(TransactionContext other) {
        if (other == null) {
            return false;
        }
        return transactionId.equals(other.transactionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionContext that = (TransactionContext) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "TransactionContext{" +
            "transactionId='" + transactionId + '\'' +
            ", transactionType=" + transactionType +
            ", isolationLevel=" + isolationLevel +
            ", state=" + state +
            ", suspended=" + suspended +
            ", participantId='" + participantId + '\'' +
            ", ageSeconds=" + getAgeSeconds() +
            '}';
    }
}
