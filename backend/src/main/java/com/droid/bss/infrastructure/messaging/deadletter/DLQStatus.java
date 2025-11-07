package com.droid.bss.infrastructure.messaging.deadletter;

/**
 * Dead Letter Queue Entry Status
 */
public enum DLQStatus {
    PENDING,
    REPROCESSED,
    RESOLVED
}
