package com.droid.bss.infrastructure.messaging.deadletter;

/**
 * Stub class for DeadLetterQueueException
 * Minimal implementation for testing purposes
 */
public class DeadLetterQueueException extends Exception {

    public DeadLetterQueueException(String message) {
        super(message);
    }

    public DeadLetterQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}
