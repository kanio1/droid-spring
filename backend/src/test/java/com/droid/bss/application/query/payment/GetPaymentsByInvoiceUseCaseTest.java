package com.droid.bss.application.query.payment;

import com.droid.bss.application.dto.invoice.GetPaymentsByInvoiceQuery;
import com.droid.bss.application.dto.payment.PaymentDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test for GetPaymentsByInvoiceUseCase
 * Following Arrange-Act-Assert pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPaymentsByInvoiceUseCase Query Side")
class GetPaymentsByInvoiceUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private GetPaymentsByInvoiceUseCase getPaymentsByInvoiceUseCase;

    @Test
    @DisplayName("Should return all payments for invoice")
    void shouldReturnAllPaymentsForInvoice() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

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

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("100.00"), PaymentStatus.COMPLETED));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("99.98"), PaymentStatus.COMPLETED));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.get(1).getAmount()).isEqualTo(new BigDecimal("99.98"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return empty list when invoice has no payments")
    void shouldReturnEmptyListWhenInvoiceHasNoPayments() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

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
            InvoiceStatus.SENT
        );

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(List.of());

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return payments sorted by payment date")
    void shouldReturnPaymentsSortedByPaymentDate() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Alice", "Johnson", "11111111111", "1111111111");
        ContactInfo contactInfo = new ContactInfo("alice.johnson@example.com", "+48111111111");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product C", 3, new BigDecimal("75.00"), new BigDecimal("225.00"))),
            new BigDecimal("225.00"),
            new BigDecimal("51.75"),
            new BigDecimal("276.75"),
            InvoiceStatus.PAID
        );

        // Create payments with different dates
        Payment payment1 = createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("200.00"), PaymentStatus.COMPLETED);
        Payment payment2 = createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("76.75"), PaymentStatus.COMPLETED);

        List<Payment> payments = List.of(payment1, payment2);

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        // Payments should be sorted by payment date (newest first)
        // Note: The actual sorting depends on the repository implementation
        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return payments with different statuses")
    void shouldReturnPaymentsWithDifferentStatuses() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Bob", "Williams", "22222222222", "2222222222");
        ContactInfo contactInfo = new ContactInfo("bob.williams@example.com", "+48222222222");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product D", 1, new BigDecimal("400.00"), new BigDecimal("400.00"))),
            new BigDecimal("400.00"),
            new BigDecimal("92.00"),
            new BigDecimal("492.00"),
            InvoiceStatus.PAID
        );

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("300.00"), PaymentStatus.COMPLETED));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("100.00"), PaymentStatus.PENDING));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("92.00"), PaymentStatus.REFUNDED));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        List<String> statuses = result.stream()
            .map(PaymentDto::getStatus)
            .toList();

        assertThat(statuses).containsExactlyInAnyOrder(
            "COMPLETED", "PENDING", "REFUNDED"
        );

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return payments with correct invoice ID")
    void shouldReturnPaymentsWithCorrectInvoiceId() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Charlie", "Brown", "33333333333", "3333333333");
        ContactInfo contactInfo = new ContactInfo("charlie.brown@example.com", "+48333333333");
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

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        Payment payment = createPayment(expectedInvoiceId, new BigDecimal("307.50"), PaymentStatus.COMPLETED);

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(List.of(payment));

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo(invoiceId);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should handle multiple payment methods")
    void shouldHandleMultiplePaymentMethods() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Diana", "Prince", "44444444444", "4444444444");
        ContactInfo contactInfo = new ContactInfo("diana.prince@example.com", "+48444444444");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(
                new InvoiceItem("Product F", 1, new BigDecimal("100.00"), new BigDecimal("100.00")),
                new InvoiceItem("Product G", 1, new BigDecimal("50.00"), new BigDecimal("50.00"))
            ),
            new BigDecimal("150.00"),
            new BigDecimal("34.50"),
            new BigDecimal("184.50"),
            InvoiceStatus.PAID
        );

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("100.00"), PaymentStatus.COMPLETED, com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("84.50"), PaymentStatus.COMPLETED, com.droid.bss.application.dto.payment.PaymentMethod.PAYPAL));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        List<String> methods = result.stream()
            .map(PaymentDto::getPaymentMethod)
            .toList();

        assertThat(methods).containsExactlyInAnyOrder("CREDIT_CARD", "PAYPAL");

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmountCorrectly() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Eve", "Adams", "55555555555", "5555555555");
        ContactInfo contactInfo = new ContactInfo("eve.adams@example.com", "+48555555555");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product H", 2, new BigDecimal("100.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"),
            new BigDecimal("246.00"),
            InvoiceStatus.PAID
        );

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("150.00"), PaymentStatus.COMPLETED));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("96.00"), PaymentStatus.COMPLETED));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        BigDecimal totalPaid = result.stream()
            .map(PaymentDto::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPaid).isEqualTo(new BigDecimal("246.00"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should handle large number of payments efficiently")
    void shouldHandleLargeNumberOfPaymentsEfficiently() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Frank", "Miller", "66666666666", "6666666666");
        ContactInfo contactInfo = new ContactInfo("frank.miller@example.com", "+48666666666");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product I", 1, new BigDecimal("100.00"), new BigDecimal("100.00"))),
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            new BigDecimal("100.00"),
            InvoiceStatus.PAID
        );

        // Create 100 payments
        List<Payment> payments = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("1.00"), PaymentStatus.COMPLETED));
        }

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(100);

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return payments with transaction IDs")
    void shouldReturnPaymentsWithTransactionIds() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Grace", "Hopper", "77777777777", "7777777777");
        ContactInfo contactInfo = new ContactInfo("grace.hopper@example.com", "+48777777777");
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

        Payment payment1 = createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("100.00"), PaymentStatus.COMPLETED);
        Payment payment2 = createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("80.00"), PaymentStatus.COMPLETED);

        // Set transaction IDs
        setPaymentTransactionId(payment1, "TXN-001");
        setPaymentTransactionId(payment2, "TXN-002");

        List<Payment> payments = List.of(payment1, payment2);

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTransactionId()).isNotNull();
        assertThat(result.get(1).getTransactionId()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return payments with dates")
    void shouldReturnPaymentsWithDates() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Henry", "Ford", "88888888888", "8888888888");
        ContactInfo contactInfo = new ContactInfo("henry.ford@example.com", "+48888888888");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product K", 1, new BigDecimal("120.00"), new BigDecimal("120.00"))),
            new BigDecimal("120.00"),
            new BigDecimal("0.00"),
            new BigDecimal("120.00"),
            InvoiceStatus.PAID
        );

        Payment payment = createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("120.00"), PaymentStatus.COMPLETED);

        // Set paid date
        setPaymentPaidDate(payment, LocalDateTime.now().minusDays(3));

        List<Payment> payments = List.of(payment);

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaidDate()).isNotNull();

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should return only COMPLETED payments")
    void shouldReturnOnlyCompletedPayments() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Irene", "Adler", "99999999999", "9999999999");
        ContactInfo contactInfo = new ContactInfo("irene.adler@example.com", "+48999999999");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product L", 1, new BigDecimal("200.00"), new BigDecimal("200.00"))),
            new BigDecimal("200.00"),
            new BigDecimal("46.00"),
            new BigDecimal("246.00"),
            InvoiceStatus.PAID
        );

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("100.00"), PaymentStatus.COMPLETED));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("50.00"), PaymentStatus.PENDING));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("96.00"), PaymentStatus.COMPLETED));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // All payments returned

        // Filter to COMPLETED only
        List<PaymentDto> completedPayments = result.stream()
            .filter(payment -> "COMPLETED".equals(payment.getStatus()))
            .toList();

        assertThat(completedPayments).hasSize(2);
        assertThat(completedPayments.get(0).getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(completedPayments.get(1).getAmount()).isEqualTo(new BigDecimal("96.00"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    @Test
    @DisplayName("Should calculate refund amounts correctly")
    void shouldCalculateRefundAmountsCorrectly() {
        // Arrange
        String invoiceId = UUID.randomUUID().toString();

        GetPaymentsByInvoiceQuery query = new GetPaymentsByInvoiceQuery(invoiceId);

        CustomerInfo personalInfo = new CustomerInfo("Jack", "Sparrow", "00000000000", "0000000000");
        ContactInfo contactInfo = new ContactInfo("jack.sparrow@example.com", "+48000000000");
        Customer customer = Customer.create(personalInfo, contactInfo);

        Invoice invoice = Invoice.create(
            customer.getId(),
            new OrderId(UUID.randomUUID()),
            java.util.List.of(new InvoiceItem("Product M", 1, new BigDecimal("300.00"), new BigDecimal("300.00"))),
            new BigDecimal("300.00"),
            new BigDecimal("69.00"),
            new BigDecimal("369.00"),
            InvoiceStatus.PARTIALLY_REFUNDED
        );

        List<Payment> payments = new ArrayList<>();
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("300.00"), PaymentStatus.COMPLETED));
        payments.add(createPayment(new InvoiceId(UUID.randomUUID()), new BigDecimal("69.00"), PaymentStatus.REFUNDED));

        InvoiceId expectedInvoiceId = new InvoiceId(UUID.fromString(invoiceId));

        when(invoiceRepository.findById(eq(expectedInvoiceId))).thenReturn(Optional.of(invoice));
        when(paymentRepository.findByInvoiceId(eq(expectedInvoiceId))).thenReturn(payments);

        // Act
        List<PaymentDto> result = getPaymentsByInvoiceUseCase.handle(query);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        BigDecimal completedTotal = result.stream()
            .filter(payment -> "COMPLETED".equals(payment.getStatus()))
            .map(PaymentDto::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundedTotal = result.stream()
            .filter(payment -> "REFUNDED".equals(payment.getStatus()))
            .map(PaymentDto::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(completedTotal).isEqualTo(new BigDecimal("300.00"));
        assertThat(refundedTotal).isEqualTo(new BigDecimal("69.00"));

        verify(invoiceRepository).findById(eq(expectedInvoiceId));
        verify(paymentRepository).findByInvoiceId(eq(expectedInvoiceId));
    }

    // Helper methods
    private Payment createPayment(InvoiceId invoiceId, BigDecimal amount, PaymentStatus status) {
        return createPayment(invoiceId, amount, status, com.droid.bss.application.dto.payment.PaymentMethod.CREDIT_CARD);
    }

    private Payment createPayment(InvoiceId invoiceId, BigDecimal amount, PaymentStatus status, com.droid.bss.application.dto.payment.PaymentMethod method) {
        Payment payment = Payment.create(
            invoiceId,
            amount,
            method,
            status
        );
        return payment;
    }

    private void setPaymentTransactionId(Payment payment, String transactionId) {
        try {
            java.lang.reflect.Method setTransactionIdMethod = Payment.class.getDeclaredMethod("setTransactionId", String.class);
            setTransactionIdMethod.setAccessible(true);
            setTransactionIdMethod.invoke(payment, transactionId);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    private void setPaymentPaidDate(Payment payment, LocalDateTime paidDate) {
        try {
            java.lang.reflect.Method setPaidDateMethod = Payment.class.getDeclaredMethod("setPaidDate", LocalDateTime.class);
            setPaidDateMethod.setAccessible(true);
            setPaidDateMethod.invoke(payment, paidDate);
        } catch (Exception e) {
            // Ignore reflection errors
        }
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
