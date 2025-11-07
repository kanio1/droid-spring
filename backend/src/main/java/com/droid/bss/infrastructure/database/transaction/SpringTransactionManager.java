package com.droid.bss.infrastructure.database.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Stub class for Spring transaction management
 * Minimal implementation for testing purposes
 */
public class SpringTransactionManager implements PlatformTransactionManager {

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        // Stub implementation
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
        // Stub implementation - do nothing
    }

    @Override
    public void rollback(TransactionStatus status) {
        // Stub implementation - do nothing
    }

    /**
     * Simple transaction status implementation
     */
    private static class SimpleTransactionStatus implements TransactionStatus {

        private boolean completed = false;
        private boolean rollbackOnly = false;

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public Object createSavepoint() {
            return new Object();
        }

        @Override
        public void rollbackToSavepoint(Object savepoint) {
            // Stub implementation
        }

        @Override
        public void releaseSavepoint(Object savepoint) {
            // Stub implementation
        }
    }
}
