package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.RefundPaymentCommand;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.invoice.Invoice;
import com.droid.bss.domain.invoice.InvoiceId;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.payment.Payment;
import com.droid.bss.domain.payment.PaymentId;
import com.droid.bss.domain.payment.PaymentRepository;
import com.droid.bss.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for RefundPaymentUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundPaymentUseCase Application Layer")
class RefundPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private RefundPaymentUseCase refundPaymentUseCase;

    @Test
    @DisplayName("Should refund payment successfully")
    void shouldRefundPaymentSuccessfully() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Customer requested refund";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98"))),
            new BigDecimal("199.98"),
            new BigDecimal("0.00"),
            new BigDecimal("199.98"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("199.98"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("199.98"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundReason()).isEqualTo(reason);
        assertThat(result.getRefundedDate()).isNotNull();

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should refund full payment amount")
    void shouldRefundFullPaymentAmount() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Product not as described";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product B", 1, new BigDecimal("150.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("34.50"),
            new BigDecimal("184.50"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("184.50"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("184.50"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("184.50"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundReason()).isEqualTo(reason);

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should refund partial payment amount")
    void shouldRefundPartialPaymentAmount() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Partial refund - one item returned";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(
                new InvoiceItem("Product C", 2, new BigDecimal("100.00"), new BigDecimal("200.00")),
                new InvoiceItem("Product D", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
            ),
            new BigDecimal("250.00"),
            new BigDecimal("57.50"),
            new BigDecimal("307.50"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("307.50"),
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("150.00"), // Partial refund
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.PARTIALLY_REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(result.getRefundReason()).isEqualTo(reason);

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            "Customer requested refund"
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            refundPaymentUseCase.handle(command);
        }, "Should throw exception when payment not found");

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to refund already refunded payment")
    void shouldThrowExceptionWhenTryingToRefundAlreadyRefundedPayment() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            "Double refund attempt"
        );

        Payment alreadyRefundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("100.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.REFUNDED
        );

        // Set refunded date
        try {
            java.lang.reflect.Method setRefundedDateMethod = Payment.class.getDeclaredMethod("setRefundedDate", LocalDateTime.class);
            setRefundedDateMethod.setAccessible(true);
            setRefundedDateMethod.invoke(alreadyRefundedPayment, LocalDateTime.now().minusDays(1));
        } catch (Exception e) {
            // Ignore reflection errors
        }

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(alreadyRefundedPayment));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            refundPaymentUseCase.handle(command);
        }, "Should throw exception when payment already refunded");

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to refund pending payment")
    void shouldThrowExceptionWhenTryingToRefundPendingPayment() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            "Refund pending payment"
        );

        Payment pendingPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("200.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER,
            PaymentStatus.PENDING
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(pendingPayment));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            refundPaymentUseCase.handle(command);
        }, "Should throw exception when trying to refund pending payment");

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to refund failed payment")
    void shouldThrowExceptionWhenTryingToRefundFailedPayment() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            "Refund failed payment"
        );

        Payment failedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("150.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.FAILED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(failedPayment));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            refundPaymentUseCase.handle(command);
        }, "Should throw exception when trying to refund failed payment");

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should record refunded date when refund is processed")
    void shouldRecordRefundedDateWhenRefundIsProcessed() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        LocalDateTime beforeRefund = LocalDateTime.now();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            "Customer requested refund - defective product"
        );

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product E", 1, new BigDecimal("250.00"), new BigDecimal("250.00"))),
            new BigDecimal("250.00"),
            new BigDecimal("57.50"),
            new BigDecimal("307.50"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("307.50"),
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("307.50"),
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getRefundedDate()).isNotNull();
        assertThat(result.getRefundedDate()).isAfter(beforeRefund);
        assertThat(result.getRefundedDate()).isBefore(LocalDateTime.now().plusSeconds(1));

        verify(paymentRepository).save(argThat(payment ->
            payment.getRefundedDate() != null &&
            payment.getRefundedDate().isAfter(beforeRefund)
        ));

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should update invoice status when fully refunded")
    void shouldUpdateInvoiceStatusWhenFullyRefunded() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Full refund - order cancelled";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product F", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"),
            new BigDecimal("246.00"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("246.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("246.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        // Invoice should be marked as REFUNDED
        verify(invoiceRepository).save(argThat(inv ->
            inv.getStatus() == InvoiceStatus.REFUNDED
        ));

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should update invoice status to PARTIALLY_PAID when partially refunded")
    void shouldUpdateInvoiceStatusToPartiallyPaidWhenPartiallyRefunded() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Partial refund - one item defective";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(
                new InvoiceItem("Product G", 1, new BigDecimal("200.00"), new BigDecimal("200.00")),
                new InvoiceItem("Product H", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))
            ),
            new BigDecimal("300.00"),
            new BigDecimal("69.00"),
            new BigDecimal("369.00"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("369.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("100.00"), // Partial refund
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.PARTIALLY_REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);

        // Invoice should be marked as PARTIALLY_PAID
        verify(invoiceRepository).save(argThat(inv ->
            inv.getStatus() == InvoiceStatus.PARTIALLY_PAID
        ));

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should handle refund without reason")
    void shouldHandleRefundWithoutReason() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            null // No reason provided
        );

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product I", 1, new BigDecimal("120.00"), new BigDecimal("120.00"))),
            new BigDecimal("120.00"),
            new BigDecimal("0.00"),
            new BigDecimal("120.00"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("120.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("120.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundReason()).isNull();

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should refund payment for different payment methods")
    void shouldRefundPaymentForDifferentPaymentMethods() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String reason = "Customer changed mind";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product J", 1, new BigDecimal("180.00"), new BigDecimal("180.00"))),
            new BigDecimal("180.00"),
            new BigDecimal("0.00"),
            new BigDecimal("180.00"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("180.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED
        );

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("180.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getPaymentMethod()).isEqualTo(com.droid.bss.application.dto.payment.PaymentMethod.BANK_TRANSFER);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundReason()).isEqualTo(reason);

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should preserve original payment transaction ID")
    void shouldPreserveOriginalPaymentTransactionId() {
        // Arrange
        String paymentId = UUID.randomUUID().toString();
        String originalTransactionId = "TXN-123456789";
        String reason = "Refund requested by customer";

        RefundPaymentCommand command = new RefundPaymentCommand(
            paymentId,
            reason
        );

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product K", 1, new BigDecimal("95.00"), new BigDecimal("95.00"))),
            new BigDecimal("95.00"),
            new BigDecimal("0.00"),
            new BigDecimal("95.00"),
            InvoiceStatus.PAID
        );

        Payment payment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("95.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.COMPLETED
        );

        // Set transaction ID using reflection
        try {
            java.lang.reflect.Method setTransactionIdMethod = Payment.class.getDeclaredMethod("setTransactionId", String.class);
            setTransactionIdMethod.setAccessible(true);
            setTransactionIdMethod.invoke(payment, originalTransactionId);
        } catch (Exception e) {
            // Ignore reflection errors
        }

        PaymentId expectedPaymentId = new PaymentId(UUID.fromString(paymentId));
        Payment refundedPayment = Payment.create(
            new InvoiceId(UUID.randomUUID()),
            new BigDecimal("95.00"),
            com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL,
            PaymentStatus.REFUNDED
        );

        when(paymentRepository.findById(eq(expectedPaymentId))).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = refundPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getTransactionId()).isEqualTo(originalTransactionId);

        verify(paymentRepository).findById(eq(expectedPaymentId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // Helper classes
    private static class OrderId {
        private final UUID value;

        public OrderId(UUID value) {
            this.value = value;
        }

        public UUID getValue() {
            return value;
        }
    }

    private static class InvoiceItem {
        private final String description;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;

        public InvoiceItem(String description, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public String getDescription() {
            return description;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }
}
