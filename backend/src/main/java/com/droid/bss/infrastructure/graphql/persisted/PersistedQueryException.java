package com.droid.bss.infrastructure.graphql.persisted;

/**
 * Exception for persisted query operations
 */
public class PersistedQueryException extends RuntimeException {

    public PersistedQueryException(String message) {
        super(message);
    }

    public PersistedQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
