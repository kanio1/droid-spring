package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.ProcessPaymentCommand;
import com.droid.bss.application.dto.payment.PaymentMethod;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.invoice.Invoice;
import com.droid.bss.domain.invoice.InvoiceId;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.payment.Payment;
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
 * Test for ProcessPaymentUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentUseCase Application Layer")
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    @DisplayName("Should process payment successfully with CREDIT_CARD")
    void shouldProcessPaymentSuccessfullyWithCreditCard() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("307.48"),
            PaymentMethod.CREDIT_CARD,
            "CARD-1234-5678",
            "John Doe",
            "123",
            "12/26"
        );

        CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", "1234567890");
        ContactInfo contactInfo = new ContactInfo("john.doe@example.com", "+48123456789");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(
                new InvoiceItem("Product A", 2, new BigDecimal("99.99"), new BigDecimal("199.98")),
                new InvoiceItem("Product B", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
            ),
            new BigDecimal("249.98"),
            new BigDecimal("57.50"),
            new BigDecimal("307.48"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("307.48"),
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(expectedInvoiceId);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("307.48"));
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getTransactionId()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should process payment successfully with BANK_TRANSFER")
    void shouldProcessPaymentSuccessfullyWithBankTransfer() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("150.00"),
            PaymentMethod.BANK_TRANSFER,
            "TRF-REF-001",
            null,
            null,
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Jane", "Smith", "98765432109", "0987654321");
        ContactInfo contactInfo = new ContactInfo("jane.smith@example.com", "+48987654321");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product C", 1, new BigDecimal("150.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("150.00"),
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(result.getReference()).isEqualTo("TRF-REF-001");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should process payment successfully with PAYPAL")
    void shouldProcessPaymentSuccessfullyWithPayPal() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("99.99"),
            PaymentMethod.PAYPAL,
            "PAYPAL-ORDER-123",
            "john.doe@example.com",
            null,
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product D", 1, new BigDecimal("99.99"), new BigDecimal("99.99"))),
            new BigDecimal("99.99"),
            new BigDecimal("0.00"),
            new BigDecimal("99.99"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("99.99"),
            PaymentMethod.PAYPAL,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getReference()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should process payment with PENDING status initially")
    void shouldProcessPaymentWithPendingStatusInitially() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("200.00"),
            PaymentMethod.CREDIT_CARD,
            "CARD-5678-9012",
            "Bob Williams",
            "456",
            "06/27"
        );

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product E", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("0.00"),
            new BigDecimal("200.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("200.00"),
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.PENDING
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when invoice not found")
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "CARD-0000-0000",
            "Test User",
            "123",
            "12/25"
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            processPaymentUseCase.handle(command);
        }, "Should throw exception when invoice not found");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when invoice already paid")
    void shouldThrowExceptionWhenInvoiceAlreadyPaid() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("300.00"),
            PaymentMethod.CREDIT_CARD,
            "CARD-1111-1111",
            "Test User",
            "789",
            "03/28"
        );

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice paidInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product F", 3, new BigDecimal("100.00"), new BigDecimal("300.00"))),
            new BigDecimal("300.00"),
            new BigDecimal("0.00"),
            new BigDecimal("300.00"),
            InvoiceStatus.PAID
        );

        // Set paid date
        try {
            java.lang.reflect.Method setPaidDateMethod = Invoice.class.getDeclaredMethod("setPaidDate", LocalDateTime.class);
            setPaidDateMethod.setAccessible(true);
            setPaidDateMethod.invoke(paidInvoice, LocalDateTime.now().minusDays(1));
        } catch (Exception e) {
            // Ignore reflection errors
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(paidInvoice));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            processPaymentUseCase.handle(command);
        }, "Should throw exception when invoice already paid");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when invoice is CANCELLED")
    void shouldThrowExceptionWhenInvoiceIsCancelled() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("150.00"),
            PaymentMethod.BANK_TRANSFER,
            "TRF-REF-CANCEL",
            null,
            null,
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice cancelledInvoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product G", 1, new BigDecimal("150.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            InvoiceStatus.CANCELLED
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(cancelledInvoice));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            processPaymentUseCase.handle(command);
        }, "Should throw exception when invoice is cancelled");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception when payment amount doesn't match invoice total")
    void shouldThrowExceptionWhenPaymentAmountDoesNotMatchInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("100.00"), // Wrong amount
            PaymentMethod.CREDIT_CARD,
            "CARD-2222-2222",
            "Test User",
            "123",
            "09/25"
        );

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product H", 2, new BigDecimal("75.00"), new BigDecimal("150.00"))),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            processPaymentUseCase.handle(command);
        }, "Should throw exception when payment amount doesn't match invoice total");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid payment method")
    void shouldThrowExceptionForInvalidPaymentMethod() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("200.00"),
            null, // Invalid payment method
            "CARD-3333-3333",
            "Test User",
            "123",
            "12/24"
        );

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product I", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("0.00"),
            new BigDecimal("200.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));
        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            processPaymentUseCase.handle(command);
        }, "Should throw exception for invalid payment method");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should process partial payment")
    void shouldProcessPartialPayment() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("150.00"), // Partial payment
            PaymentMethod.CREDIT_CARD,
            "CARD-4444-4444",
            "Test User",
            "123",
            "01/27"
        );

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product J", 3, new BigDecimal("100.00"), new BigDecimal("300.00"))),
            new BigDecimal("300.00"),
            new BigDecimal("0.00"),
            new BigDecimal("300.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("150.00"),
            PaymentMethod.CREDIT_CARD,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("150.00"));
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // Invoice should be marked as PARTIALLY_PAID
        verify(invoiceRepository).save(argThat(inv ->
            inv.getStatus() == InvoiceStatus.PARTIALLY_PAID
        ));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should update invoice status to PAID when fully paid")
    void shouldUpdateInvoiceStatusToPaidWhenFullyPaid() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("250.00"),
            PaymentMethod.BANK_TRANSFER,
            "TRF-REF-FULL",
            null,
            null,
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Henry", "Ford", "88888888888", "8888888888");
        ContactInfo contactInfo = new ContactInfo("henry.ford@example.com", "+48888888888");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product K", 1, new BigDecimal("250.00"), new BigDecimal("250.00"))),
            new BigDecimal("250.00"),
            new BigDecimal("0.00"),
            new BigDecimal("250.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("250.00"),
            PaymentMethod.BANK_TRANSFER,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // Invoice should be marked as PAID
        verify(invoiceRepository).save(argThat(inv ->
            inv.getStatus() == InvoiceStatus.PAID &&
            inv.getPaidDate() != null
        ));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should record payment date")
    void shouldRecordPaymentDate() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();
        LocalDateTime beforePayment = LocalDateTime.now();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
            invoiceId,
            new BigDecimal("180.00"),
            PaymentMethod.PAYPAL,
            "PAYPAL-ORDER-999",
            "customer@example.com",
            null,
            null
        );

        CustomerInfo personalInfo = new CustomerInfo("Irene", "Adler", "99999999999", "9999999999");
        ContactInfo contactInfo = new ContactInfo("irene.adler@example.com", "+48999999999");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product L", 2, new BigDecimal("90.00"), new BigDecimal("180.00"))),
            new BigDecimal("180.00"),
            new BigDecimal("0.00"),
            new BigDecimal("180.00"),
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment expectedPayment = Payment.create(
            expectedInvoiceId,
            new BigDecimal("180.00"),
            PaymentMethod.PAYPAL,
            PaymentStatus.COMPLETED
        );

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(expectedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // Act
        Payment result = processPaymentUseCase.handle(command);

        // Assert
        assertThat(result.getPaidDate()).isNotNull();
        assertThat(result.getPaidDate()).isAfter(beforePayment);
        assertThat(result.getPaidDate()).isBefore(LocalDateTime.now().plusSeconds(1));

        verify(paymentRepository).save(argThat(payment ->
            payment.getPaidDate() != null &&
            payment.getPaidDate().isAfter(beforePayment)
        ));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
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
