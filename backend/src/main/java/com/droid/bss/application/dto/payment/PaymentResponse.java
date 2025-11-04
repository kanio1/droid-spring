package com.droid.bss.application.dto.payment;

import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentMethod;
import com.droid.bss.domain.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for Payment entity
 */
@Schema(name = "PaymentResponse", description = "Payment response with full details")
public record PaymentResponse(
    String id,
    String paymentNumber,
    String customerId,
    String invoiceId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String paymentMethodDisplayName,
    String paymentStatus,
    String paymentStatusDisplayName,
    String transactionId,
    String gateway,
    LocalDate paymentDate,
    LocalDate receivedDate,
    String referenceNumber,
    String notes,
    String reversalReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version
) {

    public static PaymentResponse from(PaymentEntity payment) {
        return new PaymentResponse(
            payment.getId().toString(),
            payment.getPaymentNumber(),
            payment.getCustomer().getId().toString(),
            payment.getInvoice() != null ? payment.getInvoice().getId().toString() : null,
            payment.getAmount(),
            payment.getCurrency(),
            payment.getPaymentMethod().name(),
            getPaymentMethodDisplayName(payment.getPaymentMethod()),
            payment.getPaymentStatus().name(),
            getPaymentStatusDisplayName(payment.getPaymentStatus()),
            payment.getTransactionId(),
            payment.getGateway(),
            payment.getPaymentDate(),
            payment.getReceivedDate(),
            payment.getReferenceNumber(),
            payment.getNotes(),
            payment.getReversalReason(),
            payment.getCreatedAt(),
            payment.getUpdatedAt(),
            payment.getVersion().longValue()
        );
    }

    private static String getPaymentMethodDisplayName(PaymentMethod method) {
        return switch (method) {
            case CARD -> "Karta";
            case CREDIT_CARD -> "Karta kredytowa";
            case BANK_TRANSFER -> "Przelew bankowy";
            case CASH -> "Gotówka";
            case DIRECT_DEBIT -> "Zlecenie stałe";
            case MOBILE_PAY -> "Płatność mobilna";
        };
    }

    private static String getPaymentStatusDisplayName(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Oczekuje";
            case PROCESSING -> "Przetwarzanie";
            case COMPLETED -> "Zakończone";
            case FAILED -> "Nieudane";
            case REFUNDED -> "Zwrócone";
        };
    }
}
