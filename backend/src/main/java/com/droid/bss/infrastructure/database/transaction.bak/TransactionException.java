package com.droid.bss.infrastructure.database.transaction;

/**
 * Exception thrown when transaction operations fail.
 *
 * @since 1.0
 */
public class TransactionException extends Exception {

    private final String transactionId;
    private final String errorCode;
    private final boolean rollbackOnly;

    /**
     * Creates a new TransactionException.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
        this.transactionId = null;
        this.errorCode = "TRANSACTION_ERROR";
        this.rollbackOnly = false;
    }

    /**
     * Creates a new TransactionException.
     *
     * @param transactionId the transaction ID
     * @param message the error message
     * @param cause the underlying cause
     */
    public TransactionException(String transactionId, String message, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
        this.errorCode = "TRANSACTION_ERROR";
        this.rollbackOnly = false;
    }

    /**
     * Creates a new TransactionException.
     *
     * @param transactionId the transaction ID
     * @param message the error message
     * @param errorCode the error code
     * @param rollbackOnly whether transaction is rollback-only
     * @param cause the underlying cause
     */
    public TransactionException(String transactionId, String message, String errorCode,
                                boolean rollbackOnly, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
        this.errorCode = errorCode;
        this.rollbackOnly = rollbackOnly;
    }

    /**
     * Creates a new TransactionException.
     *
     * @param transactionId the transaction ID
     * @param message the error message
     * @param errorCode the error code
     * @param rollbackOnly whether transaction is rollback-only
     */
    public TransactionException(String transactionId, String message, String errorCode, boolean rollbackOnly) {
        super(message);
        this.transactionId = transactionId;
        this.errorCode = errorCode;
        this.rollbackOnly = rollbackOnly;
    }

    /**
     * Creates an exception for transaction timeout.
     *
     * @param transactionId the transaction ID
     * @param timeoutSeconds the timeout
     * @return the exception
     */
    public static TransactionException timeout(String transactionId, long timeoutSeconds) {
        return new TransactionException(
            transactionId,
            "Transaction timeout after " + timeoutSeconds + " seconds",
            "TRANSACTION_TIMEOUT",
            true,
            null
        );
    }

    /**
     * Creates an exception for invalid transaction state.
     *
     * @param transactionId the transaction ID
     * @param state the invalid state
     * @param operation the attempted operation
     * @return the exception
     */
    public static TransactionException invalidState(String transactionId, String state, String operation) {
        return new TransactionException(
            transactionId,
            "Invalid transaction state: " + state + " for operation: " + operation,
            "INVALID_TRANSACTION_STATE",
            false,
            null
        );
    }

    /**
     * Creates an exception for commit failure.
     *
     * @param transactionId the transaction ID
     * @param cause the underlying cause
     * @return the exception
     */
    public static TransactionException commitFailure(String transactionId, Throwable cause) {
        return new TransactionException(
            transactionId,
            "Failed to commit transaction: " + cause.getMessage(),
            "COMMIT_FAILED",
            false,
            cause
        );
    }

    /**
     * Creates an exception for rollback failure.
     *
     * @param transactionId the transaction ID
     * @param cause the underlying cause
     * @return the exception
     */
    public static TransactionException rollbackFailure(String transactionId, Throwable cause) {
        return new TransactionException(
            transactionId,
            "Failed to rollback transaction: " + cause.getMessage(),
            "ROLLBACK_FAILED",
            true,
            cause
        );
    }

    /**
     * Creates an exception for connection failure.
     *
     * @param transactionId the transaction ID
     * @param message the error message
     * @return the exception
     */
    public static TransactionException connectionFailure(String transactionId, String message) {
        return new TransactionException(
            transactionId,
            "Connection failure: " + message,
            "CONNECTION_FAILURE",
            true,
            null
        );
    }

    /**
     * Creates an exception for nested transaction not supported.
     *
     * @param transactionId the transaction ID
     * @return the exception
     */
    public static TransactionException nestedTransactionsNotSupported(String transactionId) {
        return new TransactionException(
            transactionId,
            "Nested transactions are not supported",
            "NESTED_TRANSACTIONS_NOT_SUPPORTED",
            false,
            null
        );
    }

    /**
     * Creates an exception for distributed transaction failure.
     *
     * @param transactionId the transaction ID
     * @param message the error message
     * @param cause the underlying cause
     * @return the exception
     */
    public static TransactionException distributedTransactionFailure(String transactionId, String message,
                                                                      Throwable cause) {
        return new TransactionException(
            transactionId,
            "Distributed transaction failure: " + message,
            "DISTRIBUTED_TRANSACTION_FAILED",
            false,
            cause
        );
    }

    /**
     * Gets the transaction ID.
     *
     * @return the transaction ID (may be null)
     */
    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the error code.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Checks if the transaction is rollback-only.
     *
     * @return true if rollback-only
     */
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
}
