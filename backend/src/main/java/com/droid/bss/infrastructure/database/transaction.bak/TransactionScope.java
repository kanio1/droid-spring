package com.droid.bss.infrastructure.database.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Manages transaction lifecycle within a scoped block.
 *
 * @since 1.0
 */
public class TransactionScope {

    private static final Logger log = LoggerFactory.getLogger(TransactionScope.class);

    private final TransactionManager transactionManager;

    /**
     * Creates a new TransactionScope.
     *
     * @param transactionManager the transaction manager
     */
    public TransactionScope(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Executes an action within a transaction.
     *
     * @param action the action to execute
     * @return the result
     * @throws TransactionException if transaction fails
     */
    public <T> T execute(TransactionAction<T> action) throws TransactionException {
        TransactionContext context = null;
        boolean rollbackOnly = false;

        try {
            // Begin transaction
            context = transactionManager.beginTransaction();

            log.debug("Executing transaction: id={}", context.getTransactionId());

            // Execute action
            T result = action.execute(context);

            // Commit transaction
            transactionManager.commit(context);

            log.debug("Transaction committed: id={}", context.getTransactionId());

            return result;

        } catch (TransactionException e) {
            if (context != null) {
                rollbackOnly = e.isRollbackOnly();
            }
            throw e;
        } catch (Exception e) {
            if (context != null) {
                log.error("Transaction failed: id={}, error={}", context.getTransactionId(), e.getMessage(), e);

                try {
                    rollbackOnly = context.isRollbackOnly();
                    if (!rollbackOnly) {
                        transactionManager.rollback(context);
                    }
                } catch (TransactionException rollbackEx) {
                    log.error("Failed to rollback transaction: id={}", context.getTransactionId(), rollbackEx);
                }
            }
            throw new TransactionException(
                context != null ? context.getTransactionId() : null,
                "Transaction execution failed: " + e.getMessage(),
                e
            );
        } finally {
            if (context != null && context.isActive()) {
                try {
                    transactionManager.rollback(context);
                } catch (TransactionException e) {
                    log.error("Failed to rollback transaction in finally block: id={}", context.getTransactionId(), e);
                }
            }
        }
    }

    /**
     * Executes an action within a transaction with default isolation level.
     *
     * @param action the action to execute
     * @throws TransactionException if transaction fails
     */
    public void executeVoid(TransactionActionVoid action) throws TransactionException {
        execute(ctx -> {
            action.execute(ctx);
            return null;
        });
    }

    /**
     * Executes an action within a transaction with specific isolation level.
     *
     * @param isolationLevel the isolation level
     * @param action the action to execute
     * @return the result
     * @throws TransactionException if transaction fails
     */
    public <T> T execute(IsolationLevel isolationLevel, TransactionAction<T> action) throws TransactionException {
        TransactionContext context = null;

        try {
            // Begin transaction with specific isolation level
            context = transactionManager.beginTransaction(isolationLevel);

            log.debug("Executing transaction: id={}, isolation={}", context.getTransactionId(), isolationLevel);

            // Execute action
            T result = action.execute(context);

            // Commit transaction
            transactionManager.commit(context);

            log.debug("Transaction committed: id={}", context.getTransactionId());

            return result;

        } catch (TransactionException e) {
            if (context != null) {
                try {
                    transactionManager.rollback(context);
                } catch (TransactionException rollbackEx) {
                    log.error("Failed to rollback transaction: id={}", context.getTransactionId(), rollbackEx);
                }
            }
            throw e;
        } catch (Exception e) {
            if (context != null) {
                log.error("Transaction failed: id={}, error={}", context.getTransactionId(), e.getMessage(), e);

                try {
                    transactionManager.rollback(context);
                } catch (TransactionException rollbackEx) {
                    log.error("Failed to rollback transaction: id={}", context.getTransactionId(), rollbackEx);
                }
            }
            throw new TransactionException(
                context != null ? context.getTransactionId() : null,
                "Transaction execution failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Executes an action within a transaction, marking it for rollback if an exception occurs.
     *
     * @param action the action to execute
     * @return the result
     * @throws TransactionException if transaction fails
     */
    public <T> T executeWithRollback(TransactionActionWithException<T> action) throws TransactionException {
        TransactionContext context = null;

        try {
            // Begin transaction
            context = transactionManager.beginTransaction();

            log.debug("Executing transaction with rollback support: id={}", context.getTransactionId());

            // Execute action
            T result = action.execute(context);

            // Check if marked for rollback
            if (context.isRollbackOnly()) {
                log.debug("Transaction marked for rollback: id={}", context.getTransactionId());
                transactionManager.rollback(context);
                throw new TransactionException(
                    context.getTransactionId(),
                    "Transaction was marked for rollback",
                    "TRANSACTION_MARKED_ROLLBACK",
                    true,
                    null
                );
            }

            // Commit transaction
            transactionManager.commit(context);

            log.debug("Transaction committed: id={}", context.getTransactionId());

            return result;

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            if (context != null) {
                log.error("Transaction failed: id={}, error={}", context.getTransactionId(), e.getMessage(), e);

                try {
                    transactionManager.rollback(context);
                } catch (TransactionException rollbackEx) {
                    log.error("Failed to rollback transaction: id={}", context.getTransactionId(), rollbackEx);
                }
            }
            throw new TransactionException(
                context != null ? context.getTransactionId() : null,
                "Transaction execution failed: " + e.getMessage(),
                e
            );
        }
    }

    /**
     * Functional interface for transaction actions.
     */
    @FunctionalInterface
    public interface TransactionAction<T> {
        T execute(TransactionContext context) throws Exception;
    }

    /**
     * Functional interface for transaction actions with exception handling.
     */
    @FunctionalInterface
    public interface TransactionActionWithException<T> {
        T execute(TransactionContext context) throws TransactionException, Exception;
    }

    /**
     * Functional interface for void transaction actions.
     */
    @FunctionalInterface
    public interface TransactionActionVoid {
        void execute(TransactionContext context) throws Exception;
    }
}
