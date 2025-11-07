package com.droid.bss.domain.payment;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.invoice.InvoiceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment - Aggregate Root Tests")
class PaymentTest {

    @Test
    @DisplayName("should create payment for invoice")
    void shouldCreatePaymentForInvoice() {
        // Given
        CustomerId customerId = CustomerId.generate();
        InvoiceId invoiceId = InvoiceId.generate();
        BigDecimal amount = new BigDecimal("250.00");
        String paymentNumber = "PAY-001";
        String transactionId = "TXN-123456";

        // When
        Payment payment = Payment.createForInvoice(
            paymentNumber,
            customerId,
            invoiceId,
            amount,
            PaymentMethod.CARD,
            transactionId,
            "STRIPE"
        );

        // Then
        assertThat(payment).isNotNull();
        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getPaymentNumber()).isEqualTo(paymentNumber);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(payment.getAmount()).isEqualByComparingTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        assertThat(payment.getGateway()).isEqualTo("STRIPE");
        assertThat(payment.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("should create standalone payment")
    void shouldCreateStandalonePayment() {
        // Given
        CustomerId customerId = CustomerId.generate();
        BigDecimal amount = new BigDecimal("100.00");
        String paymentNumber = "PAY-002";
        String transactionId = "TXN-789012";

        // When
        Payment payment = Payment.create(
            paymentNumber,
            customerId,
            amount,
            PaymentMethod.BANK_TRANSFER,
            transactionId
        );

        // Then
        assertThat(payment).isNotNull();
        assertThat(payment.getPaymentNumber()).isEqualTo(paymentNumber);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getInvoiceId()).isNull();
        assertThat(payment.getAmount()).isEqualByComparingTo(amount);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.isForInvoice()).isFalse();
    }

    @Test
    @DisplayName("should create payment with notes")
    void shouldCreatePaymentWithNotes() {
        // Given
        CustomerId customerId = CustomerId.generate();
        BigDecimal amount = new BigDecimal("150.00");
        String paymentNumber = "PAY-003";
        String referenceNumber = "REF-456789";
        String notes = "Customer requested priority processing";

        // When
        Payment payment = Payment.create(
            paymentNumber,
            customerId,
            null,
            amount,
            "PLN",
            PaymentMethod.CASH,
            null,
            null,
            referenceNumber,
            notes
        );

        // Then
        assertThat(payment.getReferenceNumber()).isEqualTo(referenceNumber);
        assertThat(payment.getNotes()).isEqualTo(notes);
    }

    @Test
    @DisplayName("should change payment status from PENDING to PROCESSING")
    void shouldChangePaymentStatusFromPendingToProcessing() {
        // Given
        Payment payment = createTestPayment();

        // When
        Payment processing = payment.changeStatus(PaymentStatus.PROCESSING);

        // Then
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(processing.getVersion()).isEqualTo(2);
        assertThat(payment.getVersion()).isEqualTo(1); // Original unchanged
    }

    @Test
    @DisplayName("should complete payment")
    void shouldCompletePayment() {
        // Given
        Payment payment = createTestPayment();

        // When
        Payment completed = payment.complete();

        // Then
        assertThat(completed.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completed.getReceivedDate()).isNotNull();
        assertThat(completed.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should fail payment")
    void shouldFailPayment() {
        // Given
        Payment payment = createTestPayment();

        // When
        Payment failed = payment.fail();

        // Then
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failed.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should refund payment")
    void shouldRefundPayment() {
        // Given
        Payment payment = createTestPayment().complete();
        String reason = "Customer request - service not delivered";

        // When
        Payment refunded = payment.refund(reason);

        // Then
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getReversalReason()).isEqualTo(reason);
        assertThat(refunded.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should add notes to payment")
    void shouldAddNotesToPayment() {
        // Given
        Payment payment = createTestPayment();
        String newNotes = "Follow up required - customer called";

        // When
        Payment withNotes = payment.addNotes(newNotes);

        // Then
        assertThat(withNotes.getNotes()).isEqualTo(newNotes);
        assertThat(withNotes.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if payment is pending")
    void shouldCheckIfPaymentIsPending() {
        // Given
        Payment pendingPayment = createTestPayment();

        // Then
        assertThat(pendingPayment.isPending()).isTrue();
        assertThat(pendingPayment.isCompleted()).isFalse();
        assertThat(pendingPayment.isFailed()).isFalse();
        assertThat(pendingPayment.isRefunded()).isFalse();
    }

    @Test
    @DisplayName("should check if payment is completed")
    void shouldCheckIfPaymentIsCompleted() {
        // Given
        Payment completedPayment = createTestPayment().complete();

        // Then
        assertThat(completedPayment.isCompleted()).isTrue();
        assertThat(completedPayment.isPending()).isFalse();
    }

    @Test
    @DisplayName("should check if payment is for invoice")
    void shouldCheckIfPaymentIsForInvoice() {
        // Given
        Payment invoicePayment = createInvoicePayment();
        Payment standalonePayment = Payment.create(
            "PAY-STANDALONE",
            CustomerId.generate(),
            new BigDecimal("100.00"),
            PaymentMethod.CASH,
            "TXN-STANDALONE"
        );

        // Then
        assertThat(invoicePayment.isForInvoice()).isTrue();
        assertThat(standalonePayment.isForInvoice()).isFalse();
    }

    @Test
    @DisplayName("should check if payment can be modified")
    void shouldCheckIfPaymentCanBeModified() {
        // Given
        Payment pendingPayment = createTestPayment();
        Payment processingPayment = pendingPayment.changeStatus(PaymentStatus.PROCESSING);
        Payment completedPayment = processingPayment.complete();

        // Then
        assertThat(pendingPayment.canBeModified()).isTrue();
        assertThat(processingPayment.canBeModified()).isTrue();
        assertThat(completedPayment.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("should check if payment can be refunded")
    void shouldCheckIfPaymentCanBeRefunded() {
        // Given
        Payment pendingPayment = createTestPayment();
        Payment completedPayment = pendingPayment.complete();

        // Then
        assertThat(pendingPayment.canBeRefunded()).isFalse();
        assertThat(completedPayment.canBeRefunded()).isTrue();
    }

    @Test
    @DisplayName("should validate status transition from PENDING")
    void shouldValidateStatusTransitionFromPending() {
        // Given
        Payment payment = createTestPayment();

        // When & Then - Valid transitions
        assertThatCode(() -> payment.changeStatus(PaymentStatus.PROCESSING))
            .doesNotThrowAnyException();
        assertThatCode(() -> payment.changeStatus(PaymentStatus.COMPLETED))
            .doesNotThrowAnyException();
        assertThatCode(() -> payment.changeStatus(PaymentStatus.FAILED))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        Payment payment = createTestPayment();

        // When & Then
        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.REFUNDED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid transition from PENDING to REFUNDED");
    }

    @Test
    @DisplayName("should throw exception for refunding non-completed payment")
    void shouldThrowExceptionForRefundingNonCompletedPayment() {
        // Given
        Payment payment = createTestPayment();

        // When & Then
        assertThatThrownBy(() -> payment.refund("Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only completed payments can be refunded");
    }

    @Test
    @DisplayName("should throw exception for transition from COMPLETED")
    void shouldThrowExceptionForTransitionFromCompleted() {
        // Given
        Payment payment = createTestPayment().complete();

        // When & Then
        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.REFUNDED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot transition from COMPLETED");
    }

    @Test
    @DisplayName("should throw exception for transition from REFUNDED")
    void shouldThrowExceptionForTransitionFromRefunded() {
        // Given
        Payment payment = createTestPayment().complete().refund("Test");

        // When & Then
        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.CANCELLED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot transition from REFUNDED");
    }

    @Test
    @DisplayName("should throw exception for transition from CANCELLED")
    void shouldThrowExceptionForTransitionFromCancelled() {
        // Given
        Payment payment = createTestPayment().changeStatus(PaymentStatus.CANCELLED);

        // When & Then
        assertThatThrownBy(() -> payment.changeStatus(PaymentStatus.COMPLETED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot transition from CANCELLED");
    }

    @Test
    @DisplayName("should throw exception for negative amount")
    void shouldThrowExceptionForNegativeAmount() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "PAY-NEG",
            customerId,
            new BigDecimal("-100.00"),
            PaymentMethod.CARD,
            "TXN-NEG"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Amount must be positive");
    }

    @Test
    @DisplayName("should throw exception for zero amount")
    void shouldThrowExceptionForZeroAmount() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "PAY-ZERO",
            customerId,
            BigDecimal.ZERO,
            PaymentMethod.CARD,
            "TXN-ZERO"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Amount must be positive");
    }

    @Test
    @DisplayName("should throw exception for null payment number")
    void shouldThrowExceptionForNullPaymentNumber() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            null,
            customerId,
            new BigDecimal("100.00"),
            PaymentMethod.CARD,
            "TXN-NULL"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment number cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null customer ID")
    void shouldThrowExceptionForNullCustomerId() {
        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "PAY-NULL-CUST",
            null,
            new BigDecimal("100.00"),
            PaymentMethod.CARD,
            "TXN-NULL-CUST"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Customer ID cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "PAY-NULL-AMT",
            customerId,
            null,
            PaymentMethod.CARD,
            "TXN-NULL-AMT"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Amount cannot be null");
    }

    @Test
    @DisplayName("should throw exception for null payment method")
    void shouldThrowExceptionForNullPaymentMethod() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Payment.create(
            "PAY-NULL-METHOD",
            customerId,
            new BigDecimal("100.00"),
            null,
            "TXN-NULL-METHOD"
        )).isInstanceOf(NullPointerException.class)
          .hasMessage("Payment method cannot be null");
    }

    @Test
    @DisplayName("should use default currency PLN")
    void shouldUseDefaultCurrencyPln() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When
        Payment payment = Payment.create(
            "PAY-CURRENCY",
            customerId,
            new BigDecimal("100.00"),
            PaymentMethod.CARD,
            "TXN-CURRENCY"
        );

        // Then
        assertThat(payment.getCurrency()).isEqualTo("PLN");
    }

    @Test
    @DisplayName("should set default payment date to today")
    void shouldSetDefaultPaymentDateToToday() {
        // Given
        CustomerId customerId = CustomerId.generate();
        LocalDate today = LocalDate.now();

        // When
        Payment payment = Payment.create(
            "PAY-DATE",
            customerId,
            new BigDecimal("100.00"),
            PaymentMethod.CARD,
            "TXN-DATE"
        );

        // Then
        assertThat(payment.getPaymentDate()).isEqualTo(today);
    }

    // Helper methods
    private Payment createTestPayment() {
        return Payment.create(
            "PAY-TEST-001",
            CustomerId.generate(),
            new BigDecimal("200.00"),
            PaymentMethod.CARD,
            "TXN-TEST-001"
        );
    }

    private Payment createInvoicePayment() {
        return Payment.createForInvoice(
            "PAY-INV-001",
            CustomerId.generate(),
            InvoiceId.generate(),
            new BigDecimal("300.00"),
            PaymentMethod.CREDIT_CARD,
            "TXN-INV-001",
            "PAYPAL"
        );
    }
}
