package com.droid.bss.domain.payment;

/**
 * Payment status enumeration
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}
