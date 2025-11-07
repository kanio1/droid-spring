package com.droid.bss.application.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Command to refund a payment
 */
@Schema(name = "RefundPaymentCommand", description = "Command to refund a payment")
public record RefundPaymentCommand(

    @Schema(description = "Payment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull
    String paymentId,

    @Schema(description = "Refund amount", example = "99.99")
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal refundAmount,

    @Schema(description = "Whether this is a full refund", example = "true")
    boolean fullRefund,

    @Schema(description = "Reason for refund", example = "Customer requested refund")
    String reason,

    @Schema(description = "User performing the refund", example = "admin@example.com")
    String performedBy,

    @Schema(description = "Refund date", example = "2025-11-07")
    LocalDate refundDate
) {
    /**
     * Constructor for full refund
     */
    public RefundPaymentCommand(String paymentId, BigDecimal refundAmount, boolean fullRefund,
                               String reason, String performedBy, LocalDate refundDate) {
        this.paymentId = paymentId;
        this.refundAmount = refundAmount;
        this.fullRefund = fullRefund;
        this.reason = reason;
        this.performedBy = performedBy;
        this.refundDate = refundDate != null ? refundDate : LocalDate.now();
    }

    /**
     * Checks if this is a full refund
     */
    public boolean isFullRefund() {
        return fullRefund;
    }

    /**
     * Gets the refund amount
     */
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    /**
     * Checks if this is a full refund (alias for isFullRefund)
     */
    public boolean fullRefund() {
        return fullRefund;
    }
}
