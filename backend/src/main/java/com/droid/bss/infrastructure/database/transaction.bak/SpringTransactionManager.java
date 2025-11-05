package com.droid.bss.infrastructure.database.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Spring-based transaction manager implementation.
 *
 * @since 1.0
 */
public class SpringTransactionManager implements TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(SpringTransactionManager.class);

    private final PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentMap<String, TransactionContext> activeTransactions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Thread> transactionThreads = new ConcurrentHashMap<>();

    private volatile int defaultTimeout = 300; // 5 minutes

    /**
     * Creates a new SpringTransactionManager.
     *
     * @param platformTransactionManager the Spring transaction manager
     * @param dataSource the data source
     */
    public SpringTransactionManager(PlatformTransactionManager platformTransactionManager, DataSource dataSource) {
        this.platformTransactionManager = platformTransactionManager;
        this.dataSource = dataSource;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    @Override
    public TransactionContext beginTransaction() throws TransactionException {
        return beginTransaction(IsolationLevel.DEFAULT);
    }

    @Override
    public TransactionContext beginTransaction(IsolationLevel isolationLevel) throws TransactionException {
        try {
            // Determine transaction type
            TransactionType transactionType = TransactionType.LOCAL;

            // Create transaction context
            String participantId = getParticipantId();
            TransactionContext context = new TransactionContext(transactionType, isolationLevel, participantId);

            // Store transaction
            activeTransactions.put(context.getTransactionId(), context);
            transactionThreads.put(context.getTransactionId(), Thread.currentThread());

            log.debug("Started transaction: id={}, isolation={}, type={}",
                context.getTransactionId(), isolationLevel, transactionType);

            return context;

        } catch (Exception e) {
            throw new TransactionException(null, "Failed to begin transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public void commit(TransactionContext context) throws TransactionException {
        if (context == null) {
            throw new TransactionException(null, "Transaction context cannot be null", null);
        }

        String transactionId = context.getTransactionId();

        try {
            log.debug("Committing transaction: id={}", transactionId);

            // Mark as committing
            context.setState(TransactionState.COMMITTING);

            if (context.isRollbackOnly()) {
                log.debug("Transaction marked for rollback, rolling back: id={}", transactionId);
                rollback(context);
                return;
            }

            // Commit
            transactionTemplate.execute((TransactionCallback<Void>) status2 -> {
                // Transaction is already started
                return null;
            });

            // Mark as committed
            context.setState(TransactionState.COMMITTED);

            // Clean up
            cleanupTransaction(context);

            log.info("Committed transaction: id={}", transactionId);

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            throw TransactionException.commitFailure(transactionId, e);
        }
    }

    @Override
    public void rollback(TransactionContext context) throws TransactionException {
        if (context == null) {
            return;
        }

        String transactionId = context.getTransactionId();

        try {
            log.debug("Rolling back transaction: id={}", transactionId);

            // Mark as rolling back
            context.setState(TransactionState.ROLLING_BACK);

            // Rollback
            transactionTemplate.execute((TransactionCallback<Void>) status -> {
                if (!status.isCompleted()) {
                    status.setRollbackOnly();
                }
                return null;
            });

            // Mark as rolled back
            context.setState(TransactionState.ROLLED_BACK);

            // Clean up
            cleanupTransaction(context);

            log.info("Rolled back transaction: id={}", transactionId);

        } catch (TransactionException e) {
            throw e;
        } catch (Exception e) {
            throw TransactionException.rollbackFailure(transactionId, e);
        }
    }

    @Override
    public void suspend(TransactionContext context) throws TransactionException {
        if (context == null) {
            return;
        }

        String transactionId = context.getTransactionId();

        try {
            log.debug("Suspending transaction: id={}", transactionId);

            context.setState(TransactionState.SUSPENDING);
            context.setSuspended(true);

            // Remove from thread binding
            transactionThreads.remove(transactionId);

            context.setState(TransactionState.SUSPENDED);

            log.debug("Suspended transaction: id={}", transactionId);

        } catch (Exception e) {
            throw new TransactionException(transactionId, "Failed to suspend transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public void resume(TransactionContext context) throws TransactionException {
        if (context == null) {
            return;
        }

        String transactionId = context.getTransactionId();

        try {
            log.debug("Resuming transaction: id={}", transactionId);

            context.setState(TransactionState.RESUMING);

            // Re-bind to current thread
            transactionThreads.put(transactionId, Thread.currentThread());
            context.setSuspended(false);

            context.setState(TransactionState.ACTIVE);

            log.debug("Resumed transaction: id={}", transactionId);

        } catch (Exception e) {
            throw new TransactionException(transactionId, "Failed to resume transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public TransactionContext getCurrentContext() {
        Thread currentThread = Thread.currentThread();
        for (String transactionId : transactionThreads.keySet()) {
            Thread thread = transactionThreads.get(transactionId);
            if (thread != null && thread.equals(currentThread)) {
                return activeTransactions.get(transactionId);
            }
        }
        return null;
    }

    @Override
    public boolean isTransactionActive() {
        TransactionContext context = getCurrentContext();
        return context != null && context.isActive();
    }

    @Override
    public int getTransactionTimeout() {
        return defaultTimeout;
    }

    @Override
    public void setTransactionTimeout(int timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.defaultTimeout = timeout;
    }

    @Override
    public Connection getConnection(TransactionContext context) throws TransactionException {
        if (context == null) {
            throw new TransactionException(null, "Transaction context cannot be null", null);
        }

        try {
            return DataSourceUtils.getConnection(dataSource);
        } catch (Exception e) {
            throw TransactionException.connectionFailure(context.getTransactionId(), e.getMessage());
        }
    }

    @Override
    public void prepare(TransactionContext context) throws TransactionException {
        throw new TransactionException(
            context.getTransactionId(),
            "Prepare not supported for local transactions",
            "PREPARE_NOT_SUPPORTED",
            false,
            null
        );
    }

    @Override
    public void forget(TransactionContext context) throws TransactionException {
        cleanupTransaction(context);
    }

    @Override
    public boolean supportsXATransactions() {
        return false;
    }

    @Override
    public boolean supportsDistributedTransactions() {
        return false;
    }

    // Private helper methods

    private TransactionDefinition createTransactionDefinition(IsolationLevel isolationLevel) {
        return new TransactionDefinition() {
            @Override
            public String getName() {
                return "Transaction-" + System.currentTimeMillis();
            }

            @Override
            public int getIsolationLevel() {
                return isolationLevel != null ? isolationLevel.getLevel() : TransactionDefinition.ISOLATION_DEFAULT;
            }

            @Override
            public int getPropagationBehavior() {
                return TransactionDefinition.PROPAGATION_REQUIRED;
            }

            @Override
            public int getTimeout() {
                return defaultTimeout;
            }

            @Override
            public boolean isReadOnly() {
                return false;
            }
        };
    }

    private String getParticipantId() {
        return "local-transaction";
    }

    private void cleanupTransaction(TransactionContext context) {
        if (context == null) {
            return;
        }

        String transactionId = context.getTransactionId();
        activeTransactions.remove(transactionId);
        transactionThreads.remove(transactionId);
    }
}
