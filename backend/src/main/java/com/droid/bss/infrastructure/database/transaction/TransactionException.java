package com.droid.bss.infrastructure.database.transaction;

/**
 * Stub exception for transaction errors
 * Minimal implementation for testing purposes
 */
public class TransactionException extends RuntimeException {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
