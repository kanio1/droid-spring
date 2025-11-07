package com.droid.bss.application.dto.payment;

/**
 * Command for deleting a payment
 *
 * @param id the payment ID to delete
 */
public record DeletePaymentCommand(
        String id
) {
    /**
     * Validates that the ID is not null or empty
     */
    public DeletePaymentCommand {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
    }
}
