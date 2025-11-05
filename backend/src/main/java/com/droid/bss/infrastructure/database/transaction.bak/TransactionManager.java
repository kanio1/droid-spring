package com.droid.bss.infrastructure.database.transaction;

import java.sql.Connection;

/**
 * Interface for managing database transactions.
 *
 * Supports both local and distributed transactions.
 *
 * @since 1.0
 */
public interface TransactionManager {

    /**
     * Begins a new transaction.
     *
     * @return the transaction context
     * @throws TransactionException if transaction cannot be started
     */
    TransactionContext beginTransaction() throws TransactionException;

    /**
     * Begins a new transaction with specific isolation level.
     *
     * @param isolationLevel the isolation level
     * @return the transaction context
     * @throws TransactionException if transaction cannot be started
     */
    TransactionContext beginTransaction(IsolationLevel isolationLevel) throws TransactionException;

    /**
     * Commits the current transaction.
     *
     * @param context the transaction context
     * @throws TransactionException if commit fails
     */
    void commit(TransactionContext context) throws TransactionException;

    /**
     * Rolls back the current transaction.
     *
     * @param context the transaction context
     * @throws TransactionException if rollback fails
     */
    void rollback(TransactionContext context) throws TransactionException;

    /**
     * Suspends a transaction.
     *
     * @param context the transaction context
     * @throws TransactionException if suspend fails
     */
    void suspend(TransactionContext context) throws TransactionException;

    /**
     * Resumes a suspended transaction.
     *
     * @param context the transaction context
     * @throws TransactionException if resume fails
     */
    void resume(TransactionContext context) throws TransactionException;

    /**
     * Gets the current transaction context.
     *
     * @return the context (may be null)
     */
    TransactionContext getCurrentContext();

    /**
     * Checks if a transaction is active.
     *
     * @return true if transaction is active
     */
    boolean isTransactionActive();

    /**
     * Gets the transaction timeout.
     *
     * @return the timeout in seconds
     */
    int getTransactionTimeout();

    /**
     * Sets the transaction timeout.
     *
     * @param timeout the timeout in seconds
     */
    void setTransactionTimeout(int timeout);

    /**
     * Gets the underlying connection for the transaction.
     *
     * @param context the transaction context
     * @return the connection
     * @throws TransactionException if connection cannot be obtained
     */
    Connection getConnection(TransactionContext context) throws TransactionException;

    /**
     * Prepares a distributed transaction for commit (2PC).
     *
     * @param context the transaction context
     * @throws TransactionException if prepare fails
     */
    void prepare(TransactionContext context) throws TransactionException;

    /**
     * Forgets a resolved transaction.
     *
     * @param context the transaction context
     * @throws TransactionException if forget fails
     */
    void forget(TransactionContext context) throws TransactionException;

    /**
     * Checks if this transaction manager supports XA transactions.
     *
     * @return true if XA is supported
     */
    boolean supportsXATransactions();

    /**
     * Checks if this transaction manager supports distributed transactions.
     *
     * @return true if distributed transactions are supported
     */
    boolean supportsDistributedTransactions();
}
